from typing import Dict, Any
import os
import boto3
from models.processed_pdf import ProcessedPdf
import openai
import re
from lambdas.Status import Status
import urllib.parse


QUESTION_PDF_BUCKET_NAME = os.environ["QUESTION_PDF_BUCKET_NAME"]
s3_client = boto3.client('s3')


def handler(event: Dict[str, Any], _: Any):
    try:
        key = event['Records'][0]['s3']['object']['key']
        bucket = event['Records'][0]['s3']['bucket']['name']
        key = urllib.parse.unquote(key, encoding='utf-8')
        print(key, bucket)
        temp_response = s3_client.get_object(
            Bucket=bucket,
            Key=key
        )
        job_id = temp_response['Metadata']['job_id']
    except KeyError:
        return {
            "statusCode": 400
        }
    item = ProcessedPdf.get(job_id)
    item.qu_pdf_bucket = QUESTION_PDF_BUCKET_NAME
    item.question_status = Status.PROCESSING.value
    item.save()
    response = s3_client.get_object(
        Bucket=item.sum_pdf_bucket,
        Key=key
    )
    try:
        qa_text = get_question_text(response)
        # save the text in 3 bucket
        s3_client.put_object(Bucket=QUESTION_PDF_BUCKET_NAME, Key=key, Body=qa_text.encode())
        item.question_status = Status.SUCCESS.value
        item.save()
    except Exception as e:
        print(e)
        item.question_status = Status.FAILED.value
        item.save()


def get_question_text(response):
    def generate_questions_answers(text):
        # Generate prompt for OpenAI API
        prompt = "Generate a list of a couple of questions and answers to those question. Please add to every questio" \
                 "n you generate the prefix: '-Q:' and between the question and the answer put '-A:'. The questions a" \
                 "nd answers will be from the following text: " + text

        # Generate response from OpenAI API
        response = openai.Completion.create(
            engine="text-davinci-003",
            prompt=prompt,
            temperature=0.5,
            max_tokens=1024,
            top_p=1,
            frequency_penalty=0,
            presence_penalty=0
        )
        print(response.choices[0].text)

        # Parse response to extract list of question and answer pairs
        question_answer_list = response.choices[0].text.strip().split("-Q:")
        question_answer_list = list(filter(lambda t: "-A:" in t, question_answer_list))
        questions_answers = [(qa.split("-A:")[0].strip(), qa.split("-A: ")[1].strip()) for qa in question_answer_list if len(qa.split("-A:")) >= 2]

        return questions_answers
    # Generate questions and answers for each chunk
    text = str(response['Body'].read(), 'UTF-8')
    print("text: ", text)
    summeries = text.split("\n\n")
    questions_answers = []
    cur_chunk = ""
    for i, chunk in enumerate(summeries):
        print("chunk: ", chunk)
        # Ignore small chunks and section headings
        if len(chunk) < 50 or re.match("^[A-Z]{2,}$", chunk.strip()):
            continue
        cur_chunk += chunk + "\n"
        if len(cur_chunk) > 500 or i == len(summeries) - 1:
            # Generate questions and answers from chunk
            qa_chunk = generate_questions_answers(cur_chunk)
            print("result: ", qa_chunk)
            questions_answers.extend(qa_chunk)
            cur_chunk = ""
    qa_text = ""
    # text questions and answers
    for i, qa in enumerate(questions_answers):
        if len(qa) >= 2:
            qa_text += f"Question {i + 1}: {qa[0]}\n\nAnswer {i + 1}: {qa[1]}\n\n\n"
            print(f"Question {i + 1}: {qa[0]}")
            print(f"Answer {i + 1}: {qa[1]}")
        else:
            print("qa is small:", qa)
    return qa_text



from typing import Dict, Any
import os
import boto3
from models.processed_pdf import ProcessedPdf
import openai
from lambdas.Status import Status
import urllib.parse


STUDIES_PDF_BUCKET_NAME = os.environ["STUDIES_PDF_BUCKET_NAME"]
s3_client = boto3.client('s3')


def handler(event: Dict[str, Any], _: Any):
    try:
        key = event['Records'][0]['s3']['object']['key']
        bucket = event['Records'][0]['s3']['bucket']['name']
        key = urllib.parse.unquote(key, encoding='utf-8')
        response = s3_client.get_object(
            Bucket=bucket,
            Key=key
        )
        job_id = response['Metadata']['job_id']
    except KeyError:
        return {
            "statusCode": 400
        }
    item = ProcessedPdf.get(job_id)
    item.st_pdf_bucket = STUDIES_PDF_BUCKET_NAME
    item.studies_status = Status.PROCESSING.value
    item.save()
    try:
        researches_names = get_studies_text(response)
        # researches_names = 'my studies text'
        # save the text in 3 bucket
        s3_client.put_object(Bucket=STUDIES_PDF_BUCKET_NAME, Key=key, Body=researches_names.encode(), Metadata={'job_id': job_id})
        item.studies_status = Status.SUCCESS.value
        item.save()
    except Exception as e:
        print(e)
        item.studies_status = Status.FAILED.value
        item.question_status = Status.FAILED.value  # studies before questions
        item.save()


def get_studies_text(response):
    def generate_researches_links(text):
        prefix = "Return names of researches from the Internet related to topics in the following text. When ypu ment" \
                 "ion a research put it in new line with $ in the start of the line."
        first_user = f"{prefix}We’ve trained a model called ChatGPT which interacts in a conversational way. The dialogue" \
                     f" format makes it possible for ChatGPT to answer followup questions, admit its mistakes, challenge " \
                     f"incorrect premises, and reject inappropriate requests."
        first_assistant = "$'Language Models are Unsupervised Multitask Learners' by Alec Radford, Jeffrey Wu, Rewon Child, " \
                          "David Luan, Dario Amodei, and Ilya Sutskever (2019).\n\nhttps://d4mucfpksywv.cloudfront.net/better-langua" \
                          "ge-models/language_models_are_unsupervised_multitask_learners.pdf\n" \
                          "#'Recipes for Building an Open-Domain Chatbot' by Stephen Roller, Emily Dinan, Naman Goyal, Da " \
                          "Ju, Mary Williamson, Yinhan Liu, Jing Xu, Myle Ott, Kurt Shuster, Eric M. Smith, Y-Lan Boureau" \
                          ", and Jason Weston (2020)\n\nhttps://arxiv.org/abs/2004.13637"
        # Generate response from OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",
            messages=[{"role": "system", "content": "You are a helpful assistant."},
                      {"role": "user", "content": first_user},
                      {"role": "assistant", "content": first_assistant},
                      {"role": "user", "content": prefix + text},
                      ]
        )
        answers = response.choices[0].message.content.split("$")
        print("answers: ", answers[1:])
        return answers[1:]
    print("start studies")
    text = str(response['Body'].read(), 'UTF-8')
    print("len(text): ", len(text))
    summaries = text.split("\n\n")
    print("len(summaries): ", len(summaries))
    titles = []
    cur_chunk = ""
    for i, chunk in enumerate(summaries):
        print("chunk: ", chunk)
        cur_chunk += chunk + "\n"
        if len(cur_chunk) > 500 or i == len(summaries) - 1:
            titles.extend(generate_researches_links(cur_chunk))
            cur_chunk = ""
    researches_names = "\n\n".join(titles)
    return researches_names

from typing import Dict, Any
import os
import boto3
from models.processed_pdf import ProcessedPdf
from lambdas.Status import Status
import openai
import urllib.parse


SUMMARIZED_PDF_BUCKET_NAME = os.environ["SUMMARIZED_PDF_BUCKET_NAME"]
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
        print("start lambda")
        print(response)
        job_id = response['Metadata']['job_id']
    except KeyError:
        return {
            "statusCode": 400
        }
    item = ProcessedPdf.get(job_id)
    item.sum_pdf_bucket = SUMMARIZED_PDF_BUCKET_NAME
    item.summary_status = Status.PROCESSING.value
    item.save()
    try:
        summary_text = get_summary_text(response)
        s3_client.put_object(Bucket=SUMMARIZED_PDF_BUCKET_NAME, Key=key, Body=summary_text.encode(), Metadata={'job_id': job_id})
        item.summary_status = Status.SUCCESS.value
        item.save()
        # create summery and save in bucket
    except Exception as e:
        print(e)
        item.summary_status = Status.FAILED.value
        item.question_status = Status.FAILED.value
        item.studies_status = Status.FAILED.value
        item.save()


def get_summary_text(response):
    def get_summary(chunk):
        prompt = "Please generate for me a short summary for the following text" + chunk
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
        return response.choices[0].text + "\n"
    new_text = []
    print("start summary")
    text = str(response['Body'].read(), 'UTF-8')
    for i in range((len(text) // 1000) + 1):
        new_text.append(text[i * 1000:(i + 1) * 1000])
    # get summaries
    summaries = []
    for i in range((len(text) // 1000) + 1):
        print("response number", i + 1, "out of: ", (len(text) // 1000) + 1, "reponses.", i / ((len(text) // 1000) + 1),
              "%.")
        summaries.append(get_summary(text[i * 1000:(i + 1) * 1000]))
    summary_text = "".join(summaries)
    # save the text in 3 bucket
    return summary_text


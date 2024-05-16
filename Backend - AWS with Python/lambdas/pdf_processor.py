from typing import Dict, Any
import os
import boto3
from models.processed_pdf import ProcessedPdf
from lambdas.Status import Status
import urllib.parse

from PyPDF2 import PdfReader
import io

TEXT_BUCKET_NAME = os.environ["TEXT_BUCKET_NAME"]
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
        processed_key = key[:-3] + 'txt'
        # extract the job id from the metadata of the S3 object and change the db
        job_id = response['Metadata']['job_id']
    except KeyError:
        return {
            "statusCode": 400
        }
    item = ProcessedPdf.get(job_id)
    item.summary_status = Status.PROCESSING.value
    item.processed_key = processed_key
    item.save()
    try:
        # Extract text from pdf
        response_text = response['Body'].read()
        reader = PdfReader(io.BytesIO(response_text))
        pdf_text = "".join(page.extract_text() for page in reader.pages)
        # save the text in 3 bucket
        print("pdf_text: ", pdf_text)
        s3_client.put_object(Bucket=TEXT_BUCKET_NAME, Key=processed_key, Body=pdf_text.encode(),
                             Metadata={'job_id': job_id})
    except KeyError:
        item.summary_status = Status.FAILED.value
        item.save()
        return {
            "statusCode": 400
        }

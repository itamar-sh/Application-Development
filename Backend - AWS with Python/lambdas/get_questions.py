from typing import Dict, Any
import boto3
from models.processed_pdf import ProcessedPdf
from lambdas.Status import *

s3_client = boto3.client('s3')


def handler(event: Dict[str, Any], _: Any) -> Dict[str, Any]:
    try:
        if event['requestContext']['http']['method'] != 'GET':
            return {
                "statusCode": 400
            }
        job_id = event['queryStringParameters']['jobId']
    except KeyError:
        return {
            "statusCode": 400
        }
    item = ProcessedPdf.get(job_id)
    key = item.processed_key
    bucket = item.qu_pdf_bucket
    url = None
    if item.question_status == Status.SUCCESS.value:
        url = s3_client.generate_presigned_url(
            ClientMethod='get_object',
            Params={
                'Bucket': bucket,
                'Key': key
            })
    elif item.question_status == Status.FAILED.value:
        return {
            "statusCode": 400,
            'body': {
                'status': item.question_status,
            }
        }
    return {
        "statusCode": 200,
        'body': {
            'status': item.studies_status,
            'url': url
        }
    }

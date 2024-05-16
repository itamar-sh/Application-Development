import io
import os
from typing import Dict, Any
import uuid
import boto3
from models.processed_pdf import ProcessedPdf
from lambdas.Status import Status
from datetime import datetime


PDF_BUCKET_NAME = os.environ['PDF_BUCKET_NAME']
s3_client = boto3.client('s3')


def handler(event: Dict[str, Any], _: Any) -> Dict[str, Any]:
    try:
        if event['requestContext']['http']['method'] != 'POST':
            return {
                "statusCode": 400
            }
        # generation of a unique job ID
        job_id = str(uuid.uuid4())
        # generation of a presigned URL
        body = eval(event['body'])
        file_name = body["fileName"]
    except (KeyError, SyntaxError):
        return {
            "statusCode": 400
        }
    file_name = ''.join(c if c.isalnum() else '_' for c in file_name)
    metadata = {'job_id': job_id}  # add job ID to the metadata
    response = s3_client.generate_presigned_url('put_object',
                                                Params={'Bucket': PDF_BUCKET_NAME,
                                                        'Key': file_name,
                                                        'Metadata': metadata,
                                                        'ContentType': 'application/pdf'
                                                        },
                                                ExpiresIn=3600)
    # save the image in the s3 bucket
    now = datetime.now()
    current_time = now.strftime("%m/%d/%Y, %H:%M:%S")
    processed_pdf = ProcessedPdf(pdf_job_id=job_id,
                                 create_time=current_time,
                                 summary_status=Status.CREATED.value,
                                 question_status=Status.CREATED.value,
                                 studies_status=Status.CREATED.value,
                                 pdf_bucket_name=PDF_BUCKET_NAME,
                                 file_name_key=file_name)
    processed_pdf.save()
    return {
        "statusCode": 201,
        "body": {
            'url': response,
            'jobId': job_id
        }
    }

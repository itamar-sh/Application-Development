import boto3
import time
import os
from datetime import datetime
import copy
import requests

original_pdf_bucket_name = 'contentify-original-pdf-bucket'
original_text_bucket_name = 'contentify-original-text-bucket'
summarized_pdf_bucket_name = 'contentify-summarized-pdf-bucket'
studies_bucket_name = 'contentify-studies-bucket'
questions_bucket_name = 'contentify-questions-bucket'
urls = {
    "pdf_uploading_url": "https://uit455n663ekp2trteepdsszem0qwzno.lambda-url.us-west-2.on.aws/",
    "pdf_summarized_url": "https://2lre5rd475xmlof23i3qhoragm0cfvru.lambda-url.us-west-2.on.aws/",
    "pdf_questions_url": "https://mb3zvq76s5dirhoktgamc72k2m0bkvwz.lambda-url.us-west-2.on.aws/",
    "pdf_studies_url": "https://sjlyjn4qjs54jfr6myqr7pc5uu0ccueh.lambda-url.us-west-2.on.aws/",
    "get_pdfs_url": "https://d6e3ztgdojevpqc4up4xhukxdu0pjrsv.lambda-url.us-west-2.on.aws/"
}
os.environ.update(
    {
        "AWS_DEFAULT_REGION": "us-west-2",
        "PDF_BUCKET_NAME": original_pdf_bucket_name,
        "TEXT_BUCKET_NAME": original_text_bucket_name,
        "SUMMARIZED_PDF_BUCKET_NAME": summarized_pdf_bucket_name,
        "STUDIES_PDF_BUCKET_NAME": studies_bucket_name,
        "QUESTION_PDF_BUCKET_NAME": questions_bucket_name,
    }
)
from models.processed_pdf import ProcessedPdf


table_name = 'contentify-pdf'
pdf_job_id = 'my-pdf-job-id'
current_time = '0/0/0'
dynamodb = boto3.client('dynamodb')
s3_client = boto3.client('s3')
object_key = 'test_bucket'
test_text = 'test my application backend, we are going to test 9 lamnbdas, one DynamoDB, five S3 Bucket.'



test_item = {
    'pdf_job_id': {'S': pdf_job_id},
    'create_time': {'S': current_time},
    'summary_status': {'S': 'created'},
    'question_status': {'S': 'created'},
    'studies_status': {'S': 'created'},
    'file_name_key': {'S': "pdf_name"},
}


def deleteItem(test_item):
    response = dynamodb.delete_item(
        TableName=table_name,
        Key={'pdf_job_id': test_item["pdf_job_id"]}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = dynamodb.scan(
        TableName=table_name,
        ProjectionExpression='pdf_job_id'
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    items = response.get('Items', [])
    job_ids = [one_item['pdf_job_id']['S'] for one_item in items]
    assert test_item["pdf_job_id"] not in job_ids


def test_dyamodb():
    # put
    response = dynamodb.put_item(TableName=table_name, Item=test_item)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    # get
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    get_item = response.get('Item')
    assert get_item == test_item
    # update
    response = dynamodb.update_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}},
        UpdateExpression='SET summary_status = :new_summery_status',
        ExpressionAttributeValues={':new_summery_status': {'S': 'success'}}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    get_item = response.get('Item')
    assert get_item['summary_status']['S'] == 'success'
    # scan
    response = dynamodb.scan(
        TableName=table_name,
        ProjectionExpression='pdf_job_id'
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    items = response.get('Items', [])
    job_ids = [one_item['pdf_job_id']['S'] for one_item in items]
    assert pdf_job_id in job_ids
    deleteItem(test_item)


def test_processed_pdf():
    processed_pdf = ProcessedPdf(pdf_job_id=test_item["pdf_job_id"]["S"],
                                 create_time=test_item["create_time"]["S"],
                                 summary_status=test_item["summary_status"]["S"],
                                 question_status=test_item["question_status"]["S"],
                                 studies_status=test_item["studies_status"]["S"],
                                 pdf_bucket_name=os.environ['PDF_BUCKET_NAME'],
                                 file_name_key=test_item["file_name_key"]["S"])
    processed_pdf.save()
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': test_item["pdf_job_id"]}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    item = response.get('Item')
    assert item['pdf_job_id'] == test_item["pdf_job_id"]
    assert item['create_time'] == test_item["create_time"]
    assert item['summary_status'] == test_item["summary_status"]
    assert item['question_status'] == test_item["question_status"]
    assert item['studies_status'] == test_item["studies_status"]
    assert item['file_name_key'] == test_item["file_name_key"]


def testContentifyPdfS3Bucket():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=original_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = s3_client.get_object(Bucket=original_pdf_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    contents = response['Body'].read().decode('utf-8')
    assert contents == test_text
    # check if the upload trigger the questions_processor lambda
    time.sleep(10)
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    get_item = response.get('Item')
    assert get_item['summary_status']['S'] != 'created'
    deleteItem(test_item)


def testContentifyTextS3Bucket():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=original_text_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = s3_client.get_object(Bucket=original_text_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    contents = response['Body'].read().decode('utf-8')
    assert contents == test_text
    # check if the upload trigger the questions_processor lambda
    time.sleep(10)
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    get_item = response.get('Item')
    assert 'sum_pdf_bucket' in get_item
    deleteItem(test_item)


def testContentifySummarizedPdfS3Bucket():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=summarized_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = s3_client.get_object(Bucket=summarized_pdf_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    contents = response['Body'].read().decode('utf-8')
    assert contents == test_text
    # check if the upload trigger the questions_processor lambda
    time.sleep(10)
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    get_item = response.get('Item')
    assert get_item['studies_status']['S'] != 'created'
    deleteItem(test_item)


def testContentifytStudies3Bucket():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=original_text_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = s3_client.get_object(Bucket=original_text_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    contents = response['Body'].read().decode('utf-8')
    assert contents == test_text
    # check if the upload trigger the questions_processor lambda
    time.sleep(20)
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': pdf_job_id}}
    )
    get_item = response.get('Item')
    assert get_item['question_status']['S'] != 'created'
    deleteItem(test_item)


def testContentifytQuestions3Bucket():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=questions_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    response = s3_client.get_object(Bucket=questions_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    contents = response['Body'].read().decode('utf-8')
    assert contents == test_text
    deleteItem(test_item)


def test_pdf_uploader_lambda():
    response = requests.get(urls["pdf_uploading_url"], json={"body": {"fileName": "my_file_name"}})
    assert response.status_code == 400
    response = requests.delete(urls["pdf_uploading_url"], json={"body": {"fileName": "my_file_name"}})
    assert response.status_code == 400
    response = requests.post(urls["pdf_uploading_url"])
    assert response.status_code == 400
    response = requests.post(urls["pdf_uploading_url"], json={"wrong_param": "my_file_name"})
    assert response.status_code == 400
    response = requests.post(urls["pdf_uploading_url"], json={"fileName": "my_file_name"})
    assert response.status_code == 201
    legal_prefix = r'"url":"https:\/\/contentify-original-pdf-bucket.s3.amazonaws.com\/'
    assert legal_prefix in response.content.decode('utf-8')
    assert 'jobId' in response.content.decode('utf-8')
    body = eval(response.content.decode('utf-8'))
    jobId = body['jobId']
    status = 'created'
    pdf_bucket_name = original_pdf_bucket_name
    response = dynamodb.get_item(
        TableName=table_name,
        Key={'pdf_job_id': {'S': jobId}}
    )
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200
    get_item = response.get('Item')
    assert get_item['question_status']['S'] == status
    assert get_item['summary_status']['S'] == status
    assert get_item['studies_status']['S'] == status
    assert get_item['pdf_bucket_name']['S'] == pdf_bucket_name
    deleteItem(get_item)


def test_pdf_processor():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    # putting the object in the bucket is what call the lambda
    response = s3_client.put_object(Bucket=original_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in pdf bucket"
    time.sleep(10)
    response = s3_client.get_object(Bucket=original_text_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when taking item from text bucket"
    assert response['Body'].read().decode('utf-8'), "No real text was generated"
    deleteItem(test_item)


def test_summary_processor():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=original_text_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in text bucket"
    time.sleep(10)
    response = s3_client.get_object(Bucket=summarized_pdf_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when taking item from text bucket"
    assert response['Body'].read().decode('utf-8'), "No real text was summarized"
    deleteItem(test_item)


def test_studies_processor():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=summarized_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in text bucket"
    time.sleep(10)
    response = s3_client.get_object(Bucket=studies_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when taking item from text bucket"
    assert response['Body'].read().decode('utf-8'), "No real studies was generalized"
    deleteItem(test_item)


def test_questions_processor():
    test_item["sum_pdf_bucket"] = {'S': summarized_pdf_bucket_name}
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=summarized_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in summary bucket"
    time.sleep(10)
    response = s3_client.put_object(Bucket=studies_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in studies bucket"
    time.sleep(5)
    response = s3_client.get_object(Bucket=questions_bucket_name, Key=object_key)
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when taking item from text bucket"
    assert response['Body'].read().decode('utf-8'), "No real questions was generated"
    deleteItem(test_item)


def test_get_summary():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=summarized_pdf_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in summary bucket"
    response = requests.get(urls["pdf_summarized_url"], params={"jobId": pdf_job_id})
    assert response.status_code == 200
    assert response.content.decode('utf-8') == '{"url":null,"status":"created"}'
    deleteItem(test_item)


def test_get_questions():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=questions_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in summary bucket"
    response = requests.get(urls["pdf_questions_url"], params={"jobId": pdf_job_id})
    assert response.status_code == 200
    assert response.content.decode('utf-8') in ['{"url":null,"status":"created"}', '{"url":null,"status":"success"}',
                                                '{"url":null,"status":"processing"}']
    deleteItem(test_item)


def test_get_studies():
    dynamodb.put_item(TableName=table_name, Item=test_item)
    response = s3_client.put_object(Bucket=studies_bucket_name, Key=object_key, Body=test_text.encode(),
                                    Metadata={'job_id': pdf_job_id})
    assert response['ResponseMetadata']['HTTPStatusCode'] == 200, "Error when putting item in summary bucket"
    response = requests.get(urls["pdf_studies_url"], params={"jobId": pdf_job_id})
    assert response.status_code == 200
    assert response.content.decode('utf-8') == '{"url":null,"status":"created"}'
    deleteItem(test_item)


def test_get_pdfs_lambda():  # complicated test
    """
    We check the get_pdfs lambda.
    We insert pdfs to the DynamoDb and check if the function indeed return the latest 10 pdfs.
    """
    test_item = {
        'pdf_job_id': {'S': pdf_job_id},
        'create_time': {'S': ""},
        'summary_status': {'S': 'created'},
        'question_status': {'S': 'created'},
        'studies_status': {'S': 'created'},
        'file_name_key': {'S': "pdf_name"},
    }
    test_items = []
    for i in range(1, 9):
        new_test_item = copy.deepcopy(test_item)
        new_test_item["pdf_job_id"]["S"] += f"{i}"
        new_test_item["summary_status"] = {'S': 'success'}
        new_test_item["file_name_key"]["S"] += f"{i}"
        now = datetime.now()
        current_time = now.strftime("%m/%d/%Y, %H:%M:%S")
        new_test_item["create_time"]["S"] = current_time
        dynamodb.put_item(TableName=table_name, Item=new_test_item)
        test_items.append(new_test_item)
        time.sleep(1)
    time.sleep(3)
    # activate the lambda and catch the response
    response = requests.get(urls["get_pdfs_url"], params={"num": 2})
    assert response.status_code == 200
    assert response.content.decode('utf-8') == '{"jobs":["my-pdf-job-id8","my-pdf-job-id7"],"files_name":["pdf_name8","pdf_name7"]}'
    # bad request should be returned in case of no legal params
    response = requests.get(urls["get_pdfs_url"])
    assert response.status_code == 400
    for test_item in test_items:
        deleteItem(test_item)





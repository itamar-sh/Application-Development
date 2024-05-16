import os

from pynamodb.attributes import UnicodeAttribute
from pynamodb.models import Model


class ProcessedPdf(Model):
    """
    A representation of an image and its processing status.
    When the processing is completed, the ProcessedImage model should contain the S3 bucket and key
    for the processed image.
    """
    class Meta:
        region = os.environ["AWS_DEFAULT_REGION"]
        table_name = "contentify-pdf"
    pdf_job_id = UnicodeAttribute(hash_key=True)
    create_time = UnicodeAttribute()
    summary_status = UnicodeAttribute()
    question_status = UnicodeAttribute()
    studies_status = UnicodeAttribute()
    pdf_bucket_name = UnicodeAttribute(null=True)
    file_name_key = UnicodeAttribute(null=True)
    sum_pdf_bucket = UnicodeAttribute(null=True)
    qu_pdf_bucket = UnicodeAttribute(null=True)
    st_pdf_bucket = UnicodeAttribute(null=True)
    processed_key = UnicodeAttribute(null=True)


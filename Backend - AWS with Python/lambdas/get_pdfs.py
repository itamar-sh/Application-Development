from typing import Dict, Any
from models.processed_pdf import ProcessedPdf
from lambdas.Status import Status


def handler(event: Dict[str, Any], _: Any) -> Dict[str, Any]:
    try:
        number = int(event['queryStringParameters']['num'])
    except KeyError:
        return {
            "statusCode": 400
        }
    print(number)
    temp_jobs = []
    temp_files_name = []
    times = []
    items = ProcessedPdf.scan()
    print(items)
    for item in items:
        if item.summary_status == Status.SUCCESS.value:
            if item.file_name_key in temp_files_name:
                index = temp_files_name.index(item.file_name_key)
                if times[index] > item.create_time:
                    times[index] = item.create_time
                    temp_jobs[index] = item.pdf_job_id
            else:
                temp_jobs += [item.pdf_job_id]
                temp_files_name += [item.file_name_key]
                times += [item.create_time]
    latest_jobs = sorted([(num, idx) for idx, num in enumerate(times)], reverse=True)
    print(latest_jobs)
    latest_jobs = latest_jobs[: number]
    jobs = []
    files_name = []
    for i in latest_jobs:
        jobs += [temp_jobs[i[1]]]
        files_name += [temp_files_name[i[1]]]
    return {
        "statusCode": 200,
        'body': {
            'jobs': jobs,
            'files_name': files_name
        }
    }

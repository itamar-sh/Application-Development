# My-Android-Apps
Kotlin, Android Studio, MVVM, DynamoDB, AWS lambdas.


## Application Description
1) Upload pdf's to cloud.
2) Generate a summary of it.
3) Generate Questions and Answers from the content, for quizzes.
4) Find links for articles and studies related to pdf's content.


## Frontend
Written in Kotlin.<br/>
The Application has 5 screens, separated as fragments and adhere to MVVM architecute.<br/>
The first screen is a welcome screen with recycle view
of pdf’s.<br/>
The recycle works with the concept of MVVM while the data is taken and
upload to the DynamoDB on aws.<br/>
On the left screen there are 3 buttons to navigate to the three results screens.<br/>
<img
  src="images/home_series.jpg"
  title="4 Fragments connected to pdf handeling"
  style="display: inline-block; margin: 0 auto;" width="808" height="400"><br/>

The remaining 3 Fragments are much simpler, a simple scroll view that get’s his text
with a matched getter lambda specific bucket using url.<br/>
<img
  src="images/combined logc.png"
  style="display: inline-block; margin: 0 auto;" width="579" height="430"><br/>

## Backend
The backend is based on AWS.<br/>
The data is stored using DynamoDB, five S3 Buckets.<br/>
The logic is handled using 9 AWS lambdas with python.
The app has 5 URLs to connect with AWS compnents.
<br/><img
  src="images/AWS components review.png"
  style="display: inline-block; margin: 0 auto;" width="600" height="200"><br/>

On the home screen - The last 10 pdf’s are taken from the DB and shown
to the user using get_pdfs aws lambda.
<br/><img
  src="images/Get pdfs.png"
  style="display: inline-block; margin: 0 auto;" width="260" height="200"><br/>
The uploading is to a S3 Bucket which we call pdfBucket and performed with url which we get via pdf_uploader aws lambda.
<br/><img
  src="images/PDF uploader.png"
  style="display: inline-block; margin: 0 auto;" width="260" height="200"><br/>
For every new pdf we process his data with the pdf_processing lambda which triggered after first uploading.
<br/><img
  src="images/PDF process.png"
  style="display: inline-block; margin: 0 auto;" width="280" height="200"><br/>
We want for every new text to process his data.<br/>
The summarize_processing lambda is triggered after the text is uploaded to the text bucket.<br/>
When the summarized text is inserted to the appropriate bucket, the Studies processing
is triggered and when the studies text is put into his bucket, the Q&A processing lambda
is triggered.<br/>All those stages work closely with the statuses of the item in the DB.
<br/><img
  src="images/Logic Process.png"
  style="display: inline-block; margin: 0 auto;" width="520" height="200"><br/>

The Summarized Fragment use the get_summary lambda that retrieve text from SummerizedBucket.<br/>
In the same way we have Questions Fragment with get_question lambda and question bucket.<br/>
At last we have the studies Fragment with get_studies and studies bucket.<br/>
The app knows if the data is ready via the status from the DynamoDB.<br/>
<br/><img
  src="images/Gets logic.png"
  style="display: inline-block; margin: 0 auto;" width="255" height="207"><br/>


## The Challenge
Our first idea was to take NN models and do a fine tuning, after short
consultation and searching we understood it’s not easy task, especially the Question
and Answers part.<br/>
Then we tried To use trained models, it took a lot of time to find the
correct ones and make the connections and adaptions to our goal.<br/>
Lastly, after we found a solution, we encounter a problem of deploying those big models to aws.<br/>
The fact that aws lambda runs on Linux was the final straw, the models and libraries were
problematic to import.<br/>
After all this story we chose to use only Chat GPT through Openai API.

## Common Questions
#### Q: What are the advantages of Kotlin over other languages, compare it to java in realtion to application development?
**A:**<br/>
*Null safety:* Kotlin eliminates null pointer exceptions with its type system.<br/>
*Concise syntax:* Kotlin reduces boilerplate code, making code easier to read and write.<br/>
*Interoperability:* Kotlin seamlessly interoperates with Java, allowing gradual migration.<br/>
*Coroutines:* Kotlin simplifies asynchronous programming with lightweight coroutines.<br/>
*Smart casts:* Kotlin automatically casts variables to the correct type after checks.<br/>
*Extension functions:* Kotlin allows adding functionality to existing classes without inheriting them.<br/>
For example: ???

#### Q: Explaon what is lambda function in aws?
**A:**<br/>
A Lambda function in AWS is a serverless compute service.
It allows you to run code without provisioning or managing servers.
You can execute your code in response to events or triggers, such as changes in data, shifts in system state and in my case HTTP request.

#### Q: Explaon how the data is moving from the app to the cloud, especially how the pdf is uploaded to the cloud?
**A:**<br/>
The pdf is stored in S3 Bucket, we are able to upload it via designated url.
How we get this designated url? We get it as HTTP response from an aws lambfa.
We have a url that accept specific http requests, and he returnes back the designated url for the S3 Bucket.
The url for the aws lambda is hard coded in our code. In the future we will store it with password and keys under some DB.

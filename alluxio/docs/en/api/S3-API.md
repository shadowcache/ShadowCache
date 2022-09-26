---
layout: global
title: S3 API
nickname: S3 API
group: Client APIs
priority: 1
---

* Table of Contents
{:toc}

Alluxio supports a [RESTful API](https://docs.alluxio.io/os/restdoc/{{site.ALLUXIO_MAJOR_VERSION}}/proxy/index.html)
that is compatible with the basic operations of the Amazon [S3 API](http://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html).

The Alluxio S3 API should be used by applications designed to communicate with an S3-like storage
and would benefit from the other features provided by Alluxio, such as data caching, data
sharing with file system based applications, and storage system abstraction (e.g., using Ceph
instead of S3 as the backing store). For example, a simple application that downloads reports
generated by analytic tasks can use the S3 API instead of the more complex file system API.

There are performance implications of using the S3 API. The S3 API leverages the
Alluxio proxy, introducing an extra hop. For optimal performance, it is recommended to run the proxy
server and an Alluxio worker on each compute node. It is also recommended to put all the proxy
servers behind a load balancer.

## Features support
The following table describes the support status for current Amazon S3 functional features:

<table class="table table-striped">
  <tr><th>S3 Feature</th><th>Status</th></tr>
{% for item in site.data.table.s3-api-supported-operations %}
  <tr>
    <td>{{ item.S3Feature }}</td>
    <td>{{ item.Status }}</td>
  </tr>
{% endfor %}
</table>

## Language support
Alluxio S3 client supports various programming languages, such as C++, Java, Python, Golang, and Ruby.
In this documentation, we use curl REST calls and python S3 client as usage examples.

## Example Usage

### REST API
For example, you can run the following RESTful API calls to an Alluxio cluster running on localhost.
The Alluxio proxy is listening at port 39999 by default.

#### Authorization

By default, the user that is used to do any FileSystem operations is the user that was used to launch
the proxy process. This can be changed by providing the Authorization Header.

```console
$ curl -i -H "Authorization: AWS testuser:" -X PUT http://localhost:39999/api/v1/s3/testbucket0
HTTP/1.1 200 OK
Date: Tue, 02 Mar 2021 00:02:26 GMT
Content-Length: 0
Server: Jetty(9.4.31.v20200723)

$ bin/alluxio fs ls /
drwxr-xr-x  testuser                                    0       PERSISTED 03-01-2021 16:02:26:547  DIR /testbucket0

```

#### Create a bucket

```console
$ curl -i -X PUT http://localhost:39999/api/v1/s3/testbucket

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:23:18 GMT
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)
```

#### List all buckets owned by the user

Authenticating as a user is necessary to have buckets returned by this operation.

```console
$ curl -i -H "Authorization: AWS testuser:" -X GET http://localhost:39999/api/v1/s3
HTTP/1.1 200 OK
Date: Tue, 02 Mar 2021 00:06:43 GMT
Content-Type: application/xml
Content-Length: 109
Server: Jetty(9.4.31.v20200723)

<ListAllMyBucketsResult><Buckets><Bucket><Name>testbucket0</Name></Bucket></Buckets></ListAllMyBucketsResult>%
```

#### Get the bucket (listing objects)

```console
$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket

HTTP/1.1 200 OK
Date: Wed, 22 Sep 2021 07:13:37 GMT
Content-Type: application/xml
Content-Length: 193
Server: Jetty(9.4.43.v20210629)

<ListBucketResult><KeyCount>0</KeyCount><MaxKeys>1000</MaxKeys><Delimiter>/</Delimiter><EncodingType>url</EncodingType><IsTruncated>false</IsTruncated><Name>testbucket</Name></ListBucketResult>
```

#### Put an object
Assuming there is an existing file on local file system called `LICENSE`:

```console
$ curl -i -X PUT -T "LICENSE" http://localhost:39999/api/v1/s3/testbucket/testobject

HTTP/1.1 100 Continue

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:24:32 GMT
ETag: "911df44b7ff57801ca8d74568e4ebfbe"
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)
```

#### Get the object:

```console
$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket/testobject

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:24:57 GMT
Last-Modified: Tue, 18 Jun 2019 21:24:33 GMT
Content-Type: application/xml
Content-Length: 27040
Server: Jetty(9.2.z-SNAPSHOT)

.................. Content of the test file ...................
```

#### Listing a bucket with one object

```console
$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket

HTTP/1.1 200 OK
Date: Wed, 22 Sep 2021 07:15:19 GMT
Content-Type: application/xml
Content-Length: 306
Server: Jetty(9.4.43.v20210629)

<ListBucketResult><Contents><LastModified>2021-09-22T15:14:40.754Z</LastModified><Key>testobject</Key><Size>27040</Size></Contents><KeyCount>1</KeyCount><MaxKeys>1000</MaxKeys><Delimiter>/</Delimiter><EncodingType>url</EncodingType><IsTruncated>false</IsTruncated><Name>testbucket</Name></ListBucketResult>
```

#### Listing a bucket with multiple objects
You can upload more files and use the `max-keys` and `marker` as the [GET bucket request parameter](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html). For example:

```console
$ curl -i -X PUT -T "LICENSE" http://localhost:39999/api/v1/s3/testbucket/key1

HTTP/1.1 100 Continue

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:26:05 GMT
ETag: "911df44b7ff57801ca8d74568e4ebfbe"
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X PUT -T "LICENSE" http://localhost:39999/api/v1/s3/testbucket/key2

HTTP/1.1 100 Continue

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:26:28 GMT
ETag: "911df44b7ff57801ca8d74568e4ebfbe"
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X PUT -T "LICENSE" http://localhost:39999/api/v1/s3/testbucket/key3

HTTP/1.1 100 Continue

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:26:43 GMT
ETag: "911df44b7ff57801ca8d74568e4ebfbe"
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket\?max-keys\=2

HTTP/1.1 200 OK
Date: Wed, 22 Sep 2021 07:18:18 GMT
Content-Type: application/xml
Content-Length: 444
Server: Jetty(9.4.43.v20210629)

<ListBucketResult><Contents><LastModified>2021-09-22T15:17:39.579Z</LastModified><Key>key1</Key><Size>27040</Size></Contents><Contents><LastModified>2021-09-22T15:17:41.463Z</LastModified><Key>key2</Key><Size>27040</Size></Contents><KeyCount>2</KeyCount><MaxKeys>2</MaxKeys><Delimiter>/</Delimiter><EncodingType>url</EncodingType><NextMarker>/testbucket/key2</NextMarker><IsTruncated>true</IsTruncated><Name>testbucket</Name></ListBucketResult>

$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket\?max-keys\=2\&marker\=\/testbucket\/key2

HTTP/1.1 200 OK
Date: Wed, 22 Sep 2021 07:25:21 GMT
Content-Type: application/xml
Content-Length: 477
Server: Jetty(9.4.43.v20210629)

<ListBucketResult><Contents><LastModified>2021-09-22T15:17:39.579Z</LastModified><Key>key1</Key><Size>27040</Size></Contents><Contents><LastModified>2021-09-22T15:17:41.463Z</LastModified><Key>key2</Key><Size>27040</Size></Contents><Marker>/testbucekt/key2</Marker><KeyCount>2</KeyCount><MaxKeys>2</MaxKeys><Delimiter>/</Delimiter><EncodingType>url</EncodingType><NextMarker>/testbucket/key2</NextMarker><IsTruncated>true</IsTruncated><Name>testbucket</Name></ListBucketResult>
```

You can also verify those objects are represented as Alluxio files, under `/testbucket` directory.

```console
$ ./bin/alluxio fs ls -R /testbucket

-rw-r--r--  alluxio        staff                    27040       PERSISTED 06-18-2019 14:26:05:694 100% /testbucket/key1
-rw-r--r--  alluxio        staff                    27040       PERSISTED 06-18-2019 14:26:28:153 100% /testbucket/key2
-rw-r--r--  alluxio        staff                    27040       PERSISTED 06-18-2019 14:26:43:081 100% /testbucket/key3
-rw-r--r--  alluxio        staff                    27040       PERSISTED 06-18-2019 14:24:33:029 100% /testbucket/testobject
```

#### Delete objects

```console
$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket/key1

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:31:27 GMT
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket/key2

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:31:44 GMT
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket/key3

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:31:58 GMT
Server: Jetty(9.2.z-SNAPSHOT)

$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket/testobject

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:32:08 GMT
Server: Jetty(9.2.z-SNAPSHOT)
```

#### Initiate a multipart upload
Since we deleted the `testobject` in the previous command, you have to create another `testobject`
before initiating a multipart upload.

```console
$ curl -i -X POST http://localhost:39999/api/v1/s3/testbucket/testobject?uploads

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:32:36 GMT
Content-Type: application/xml
Content-Length: 133
Server: Jetty(9.2.z-SNAPSHOT)

<InitiateMultipartUploadResult><Bucket>testbucket</Bucket><Key>testobject</Key><UploadId>3</UploadId></InitiateMultipartUploadResult>
```

Note that the commands below related to multipart upload need the upload ID shown above, it's not necessarily 3.

#### Upload part

```console
$ curl -i -X PUT 'http://localhost:39999/api/v1/s3/testbucket/testobject?partNumber=1&uploadId=3'

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:33:36 GMT
ETag: "d41d8cd98f00b204e9800998ecf8427e"
Content-Length: 0
Server: Jetty(9.2.z-SNAPSHOT)
```

#### List parts

```console
$ curl -i -X GET http://localhost:39999/api/v1/s3/testbucket/testobject?uploadId=3

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:35:10 GMT
Content-Type: application/xml
Content-Length: 296
Server: Jetty(9.2.z-SNAPSHOT)

<ListPartsResult><Bucket>/testbucket</Bucket><Key>testobject</Key><UploadId>3</UploadId><StorageClass>STANDARD</StorageClass><IsTruncated>false</IsTruncated><Part><PartNumber>1</PartNumber><LastModified>2019-06-18T14:33:36.373Z</LastModified><ETag>""</ETag><Size>0</Size></Part></ListPartsResult>
```

#### Complete a multipart upload

```console
$ curl -i -X POST http://localhost:39999/api/v1/s3/testbucket/testobject?uploadId=3

HTTP/1.1 200 OK
Date: Tue, 18 Jun 2019 21:35:47 GMT
Content-Type: application/xml
Content-Length: 201
Server: Jetty(9.2.z-SNAPSHOT)

<CompleteMultipartUploadResult><Location>/testbucket/testobject</Location><Bucket>testbucket</Bucket><Key>testobject</Key><ETag>"d41d8cd98f00b204e9800998ecf8427e"</ETag></CompleteMultipartUploadResult>
```

#### Abort a multipart upload

A non-completed upload can be aborted:

```console
$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket/testobject?uploadId=3

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:37:27 GMT
Server: Jetty(9.2.z-SNAPSHOT)
```

#### Delete an empty bucket

```console
$ curl -i -X DELETE http://localhost:39999/api/v1/s3/testbucket

HTTP/1.1 204 No Content
Date: Tue, 18 Jun 2019 21:38:38 GMT
Server: Jetty(9.2.z-SNAPSHOT)
```

### Python S3 Client

Tested for Python 2.7.

#### Create a connection:
Please note you have to install boto package first.

```console
$ pip install boto
```

```python
import boto
import boto.s3.connection

conn = boto.connect_s3(
    aws_access_key_id = '',
    aws_secret_access_key = '',
    host = 'localhost',
    port = 39999,
    path = '/api/v1/s3',
    is_secure=False,
    calling_format = boto.s3.connection.OrdinaryCallingFormat(),
)
```

#### Authenticating as a user:
By default, authenticating with no access_key_id uses the user that was used to launch the proxy
as the user performing the file system actions.

Set the ```aws_access_key_id``` to a different username to perform the actions under a different user.

#### Create a bucket

```python
bucketName = 'bucket-for-testing'
bucket = conn.create_bucket(bucketName)
```

#### List all buckets owned by the user

Authenticating as a user is necessary to have buckets returned by this operation.

```python
conn = boto.connect_s3(
    aws_access_key_id = 'testuser',
    aws_secret_access_key = '',
    host = 'localhost',
    port = 39999,
    path = '/api/v1/s3',
    is_secure=False,
    calling_format = boto.s3.connection.OrdinaryCallingFormat(),
)

conn.get_all_buckets()
```

#### PUT a small object

```python
smallObjectKey = 'small.txt'
smallObjectContent = 'Hello World!'

key = bucket.new_key(smallObjectKey)
key.set_contents_from_string(smallObjectContent)
```

#### Get the small object

```python
assert smallObjectContent == key.get_contents_as_string()
```

#### Upload a large object
Create a 8MB file on local file system.

```console
$ dd if=/dev/zero of=8mb.data bs=1048576 count=8
```

Then use python S3 client to upload this as an object

```python
largeObjectKey = 'large.txt'
largeObjectFile = '8mb.data'

key = bucket.new_key(largeObjectKey)
with open(largeObjectFile, 'rb') as f:
    key.set_contents_from_file(f)
with open(largeObjectFile, 'rb') as f:
    largeObject = f.read()
```

#### Get the large object

```python
assert largeObject == key.get_contents_as_string()
```

#### Delete the objects

```python
bucket.delete_key(smallObjectKey)
bucket.delete_key(largeObjectKey)
```

#### Initiate a multipart upload

```python
mp = bucket.initiate_multipart_upload(largeObjectKey)
```

#### Upload parts

```python
import math, os

from filechunkio import FileChunkIO

# Use a chunk size of 1MB (feel free to change this)
sourceSize = os.stat(largeObjectFile).st_size
chunkSize = 1048576
chunkCount = int(math.ceil(sourceSize / float(chunkSize)))

for i in range(chunkCount):
    offset = chunkSize * i
    bytes = min(chunkSize, sourceSize - offset)
    with FileChunkIO(largeObjectFile, 'r', offset=offset, bytes=bytes) as fp:
        mp.upload_part_from_file(fp, part_num=i + 1)
```

#### Complete the multipart upload

```python
mp.complete_upload()
```

#### Abort the multipart upload

Non-completed uploads can be aborted.

```python
mp.cancel_upload()
```

#### Delete the bucket

```python
bucket.delete_key(largeObjectKey)
conn.delete_bucket(bucketName)
```
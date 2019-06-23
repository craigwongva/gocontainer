# gocontainer
This repo blends gocontainer, green, greendots-golang (master).
## Motivation
This repo shows Java green dots :8080, Go green dots :8077, slides :6060.

## Installation

In the following command, IP8080 means the IP from which you'll be demoing `gocontainer`, e.g. home or office. 
```
aws cloudformation create-stack --stack-name gocontainer --template-body file://cf.yml --region us-west-2 --parameter ParameterKey=IP8080,ParameterValue=REDACTED ParameterKey=IP22,ParameterValue=`curl 169.254.169.254/latest/meta-data/public-ipv4` ParameterKey=githubpassword,ParameterValue=REDACTED
```

Wait 12 minutes for everything to install, then 4 more minutes before green dots are enabled (I'm not sure why this delay exists).

## Usage
In the following, `gocontainer` is whatever public IP your CloudFormation just created.

All three URLs (java green dots on :8080, Golang green dots on :8077, slides on :6060) have been witnessed to return within 15 minutes.
* `http://gocontainer:8080/green/timer/dots`
* `http://gocontainer:8077`
* `http://gocontainer:6060`


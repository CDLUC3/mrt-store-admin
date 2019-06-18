reportName=$1
LOCALDIR=$(dirname $0)
cd $LOCALDIR
perl mailconfig.pl '../conf/mailconf.txt' "$reportName"

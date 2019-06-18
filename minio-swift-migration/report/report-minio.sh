BASEDIR=$(dirname $0)
cd $BASEDIR
confname=$1
perl report-generic.pl "../conf/${confname}minio.prop"

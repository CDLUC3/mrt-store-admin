#Get collection report
dbtype='stg';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Status-$DATE"
../report/report-miss.sh $dbtype

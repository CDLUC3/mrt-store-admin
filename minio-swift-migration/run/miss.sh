#Get collection report
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Status-$DATE"
../report/report-miss.sh $dbtype

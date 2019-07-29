#Get collection report
cd "$(dirname "$0")"
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Missing-Status-$DATE"
../report/report-miss.sh $dbtype  > ../out/$name.txt &

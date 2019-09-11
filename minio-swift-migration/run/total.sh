#Report totals
cd "$(dirname "$0")"
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Total-$DATE"
../report/report-total.sh $dbtype


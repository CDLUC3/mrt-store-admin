#Get collection report
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Status-$DATE"
../report/report-minio.sh $dbtype > ../out/$name.txt
../mail/mailminio.sh "$name"


#Get collection report
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Minio-Missing-Status-$DATE"
../report/report-miss.sh $dbtype  > ../out/$name.txt
cat ../out/$name.txt

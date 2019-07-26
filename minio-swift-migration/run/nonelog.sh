#Get collection report
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="No-Minio-$DATE"
../report/report-none.sh $dbtype  > ../out/$name.txt
cat ../out/$name.txt

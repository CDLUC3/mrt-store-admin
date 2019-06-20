#Get collection report
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Collection-Status-$DATE"
../report/report-all.sh $dbtype > ../out/$name.txt
../mail/mailall.sh "$name"


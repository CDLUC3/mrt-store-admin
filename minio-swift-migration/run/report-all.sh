#Get collection report
dbtype='mrt';
DATE=`date '+%Y%m%d-%H%M%S'`
name="Collection-Status-$DATE"
../report/report-all.sh $dbtype > ../out/$name.txt
../mail/mail.sh "$name"


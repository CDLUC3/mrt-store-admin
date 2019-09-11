#Report totals
cd "$(dirname "$0")"
dbtype='prd';
DATE=`date '+%Y%m%d-%H%M%S'`
name="$DATE"
../report/report-total-prop.sh $dbtype > ../out/progress/$name.prop &


mnemonic=$1;
mkdir -p in
mkdir -p out 
./readd.sh $mnemonic;
chmod 755 in/*sh
cd in
./${mnemonic}.sh > ../out/out-${mnemonic}.txt &
echo Submitted: ./${mnemonic}.sh
echo "> tail -f ../out/out-${mnemonic}.txt"
tail -f ../out/out-${mnemonic}.txt

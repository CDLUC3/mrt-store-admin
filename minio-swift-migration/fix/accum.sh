mnemonic=$1;
accver=$2
mkdir -p in
mkdir -p out 
echo "echo Begin Process: $mnemonic" >> ./in/accum.${accver}.sh
./readd.sh $mnemonic;
cat ./in/$mnemonic.sh >> ./in/accum.${accver}.sh
wc ./in/accum.sh

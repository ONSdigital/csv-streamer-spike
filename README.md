# CSV Generator Prototype

Prototype CSV generator that streams files directly from disk/S3 and filters
them according to a set of dimension filters on-the-fly.

## Compile and Run

```bash
mvn clean compile exec:java
```

## Testing

Example:

```bash
curl -v --compressed http://localhost:4567/data/Open-Data-small?dim_item_id_2=All+other+income
```

The basic API is that the name after `/data/{name}` is taken as the root name of a CSV file
in the resources folder, and then any query parameters are taken to be dimension queries: the 
parameter name is the name of the column to filter and the value is the allowed value. Each
row must match *all* of the supplied filters, but if you specify the same dimension multiple times
then it can match *any* of the given values. For example, to filter the Open-Data-small dataset
to all data whose NACE classification is 1105 and whose Prodcom Elements classification is either
"Waste Products" or "All other income", you would use:

```bash
curl -v --compressed 'http://localhost:4567/data/Open-Data-small?dim_item_id_2=All+other+income&dim_item_id_2=Waste+Products&dim_item_id_1=1105'
```

## Performance

No real performance figures yet, but initial testing is very promising: all data is streamed
essentially instantaneously. If you download the [worldcitiespop.txt.gz](http://www.maxmind.com/download/worldcities/worldcitiespop.txt.gz)
dataset and save it in resources as `worldcitiespop.csv` and restart the server, then you can run some
tests with a larger (151MB, ~3.2 million rows) dataset:

```bash
$ time curl -sS --compressed 'http://localhost:4567/data/worldcitiespop?country=gb' > /dev/null

real    0m1.736s
user    0m0.010s
sys     0m0.009s
$ wc -l src/main/resources/worldcitiespop.csv 
 3173959 src/main/resources/worldcitiespop.csv
$ echo 3173959/1.736 | bc
1828317
```

So processing at around **1.8 million rows/second**. Obviously, this is not a representative data set and this code is not
production quality, etc, etc, but this gives some rough baseline for possible performance.
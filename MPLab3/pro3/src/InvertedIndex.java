import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by zarah on 4/22/17.
 */


public class InvertedIndex {
    public static void main(String[] args) {
        try {
            Configuration conf = new Configuration();
            Job job = new Job(conf,"InvertedIndex");
            job.setJarByClass(InvertedIndex.class);
            job.setInputFormatClass(TextInputFormat.class);
            job.setMapperClass(InvertedIndexMapper.class);
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);
            job.setCombinerClass(InvertedIndexCombiner.class);
            job.setReducerClass(InvertedIndexReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job, new Path(args[0]));
            FileOutputFormat.setOutputPath(job, new Path(args[1]));
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        }
        catch (Exception e) { e.printStackTrace();}
    }
}

class InvertedIndexMapper extends Mapper<Object,Text,Text,Text>{
    @Override
    protected void map(Object key, Text value, Context context) throws IOException, InterruptedException
// default RecordReader: LineRecordReader; key: line offset; value: line string
    {
        StringTokenizer itr = new StringTokenizer(value.toString());
        FileSplit fileSplit = (FileSplit)context.getInputSplit();
        String[] split = String.valueOf(fileSplit.getPath().getName()).split("\\.");
        String filename;
        if(split.length==4)
            filename=split[0]+split[1];
        else
            filename=split[0];

        while(itr.hasMoreTokens()) {
            Text keyinfo = new Text(itr.nextToken()+':'+filename);
            context.write(keyinfo, new Text("1"));
        }
    }
}

class InvertedIndexCombiner extends Reducer<Text,Text,Text,Text>{
    protected void reduce(Text key, Iterable<Text>values ,Context context)throws IOException, InterruptedException
    {
        Text valueinfo =new Text();
        Text keyinfo = new Text();
        int sum=0;
        for(Text x:values){
            sum+= Integer.parseInt(x.toString());
        }
        int index = key.toString().indexOf(":");
        keyinfo=new Text(key.toString().substring(0,index));
        valueinfo=new Text(key.toString().substring(index+1,key.toString().length())+':'+Integer.toString(sum));
        context.write(keyinfo,valueinfo);
    }
}

class InvertedIndexReducer extends Reducer<Text,Text,Text,Text> {
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
    {
        ArrayList<String>novel = new ArrayList<String>();
        ArrayList<Integer>times = new ArrayList<Integer>();
        for(Text x : values) {
            int index = x.toString().indexOf(':');
            String novelname =  x.toString().substring(0,index);
            int count = Integer.parseInt(x.toString().substring(index+1,x.toString().length()));
            if(novel.indexOf(novelname)==-1){
                novel.add(novelname);
                times.add(count);
            }
            else{
                int temp = times.get(novel.indexOf(novelname));
                temp+=count;
                times.set(novel.indexOf(novelname),temp);
            }
        }

        double sum = 0;
        for(int i : times){
            sum+=(double)i;
        }
        double avg = sum / times.size();
        String avgf = String.format("%.2f",avg);
        StringBuilder all = new StringBuilder();
        all.append(avgf+',');
        for(int i=0; i <times.size();i++){
            all.append(novel.get(i)+':'+ Integer.toString(times.get(i))+';');
        }

        context.write(key, new Text(all.toString().substring(0,all.length()-1)));
    }
}

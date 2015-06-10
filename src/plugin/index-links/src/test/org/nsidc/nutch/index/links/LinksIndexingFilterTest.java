package org.nsidc.nutch.index.links;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlink;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.util.NutchConfiguration;

public class LinksIndexingFilterTest extends TestCase {

    Configuration conf = NutchConfiguration.create();
    LinksIndexingFilter filter = new LinksIndexingFilter();
    Metadata metadata = new Metadata();
    
    
    public void testNullDocument() throws Exception {

        NutchDocument doc = filter.filter(null, new ParseImpl("text", new ParseData(
                new ParseStatus(), "title", null, metadata)), new Text(
                "http://www.example.com/"), new CrawlDatum(), new Inlinks());

        assertEquals(doc, null);
    }


    public void testOutlinks() throws Exception {
        filter.setConf(conf);

        Outlink[] outlinks = new Outlink[2];

        outlinks[0] = new Outlink("http://www.test.com", "test");
        outlinks[1] = new Outlink("http://somesite.org/part/file", "example");

        NutchDocument doc = filter.filter(new NutchDocument(), new ParseImpl("text", new ParseData(
                new ParseStatus(), "title", outlinks, metadata)), new Text(
                "http://www.example.com/"), new CrawlDatum(), new Inlinks());

        assertEquals("http://www.test.com", doc.getField("outlinks").getValues().get(0).toString());
        assertEquals("http://somesite.org/part/file", doc.getField("outlinks").getValues().get(1).toString());
    }
    
    public void testMaxOutlinks() throws Exception {
        filter.setConf(conf);

        Outlink[] outlinks = new Outlink[3];

        outlinks[0] = new Outlink("http://www.test.com", "test");
        outlinks[1] = new Outlink("http://somesite.org/part/file", "example");
        outlinks[2] = new Outlink("http://othersite.org/part/file", "duh");
        
        filter.setMaxOutlinks(2);

        NutchDocument doc = filter.filter(new NutchDocument(), new ParseImpl("text", new ParseData(
                new ParseStatus(), "title", outlinks, metadata)), new Text(
                "http://www.example.com/"), new CrawlDatum(), new Inlinks());

        assertEquals(2, doc.getField("outlinks").getValues().size());
        filter.setMaxOutlinks(100);
    }

    public void testInlinks() throws Exception {
        filter.setConf(conf);

        Inlinks inlinks = new Inlinks();
        // First in Last out.
        inlinks.add(new Inlink("http://www.example.com", "example"));
        inlinks.add(new Inlink("http://my.inlink.com/something.txt", "something"));


        NutchDocument doc = filter.filter(new NutchDocument(), new ParseImpl("text", new ParseData(
                new ParseStatus(), "title", new Outlink[0], metadata)), new Text(
                "http://www.example.com/"), new CrawlDatum(), inlinks);

        assertEquals("http://my.inlink.com/something.txt", doc.getField("inlinks").getValues().get(0).toString());
        assertEquals("http://www.example.com", doc.getField("inlinks").getValues().get(1).toString());
        assertTrue(doc.getField("inlinks").getValues().size() == 2 );
    }

    public void testMaxInlinks() throws Exception {
        filter.setConf(conf);

        Inlinks inlinks = new Inlinks();
        // First in Last out.
        inlinks.add(new Inlink("http://www.example.com", "example"));
        inlinks.add(new Inlink("http://my.inlink.com/something.txt", "something"));
        inlinks.add(new Inlink("http://my.dummy.site.com/file.txt", "file"));
        
        filter.setMaxInlinks(2);

        NutchDocument doc = filter.filter(new NutchDocument(), new ParseImpl("text", new ParseData(
                new ParseStatus(), "title", new Outlink[0], metadata)), new Text(
                "http://www.example.com/"), new CrawlDatum(), inlinks);

        assertEquals("http://my.dummy.site.com/file.txt", doc.getField("inlinks").getValues().get(0).toString());
        assertEquals("http://my.inlink.com/something.txt", doc.getField("inlinks").getValues().get(1).toString());
        assertTrue(doc.getField("inlinks").getValues().size() == 2 );
        filter.setMaxInlinks(100);
    }

}
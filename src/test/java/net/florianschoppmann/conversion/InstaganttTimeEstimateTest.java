package net.florianschoppmann.conversion;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class InstaganttTimeEstimateTest {
    @DataProvider
    public Object[][] dataForTimeEstimate() {
        // <body> tags are added later!
        return new Object[][] {
            {"foo", "foo", null},
            {" [3] ", "", "180"},
            {"bla [1] ", "bla", "60"},
            {"Strange #_$[! [2]", "Strange #_$[!", "120"},
            {"xyz [x]", "xyz [x]", null}
        };
    }

    @Test(dataProvider = "dataForTimeEstimate")
    public void timeEstimate(String originalName, String expectedName, String expectedDuration) {
        var instaganttTimeEstimate = new InstaganttTimeEstimate(originalName);
        Assert.assertEquals(instaganttTimeEstimate.getTaskName(), expectedName);
        Assert.assertEquals(instaganttTimeEstimate.getTimeEstimateInMinutes(), expectedDuration);
    }
}

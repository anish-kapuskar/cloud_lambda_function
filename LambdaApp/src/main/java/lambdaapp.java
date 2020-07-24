
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;


import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
public class lambdaapp implements RequestHandler<SNSEvent, Object> {

    static DynamoDB dynamoDB;
    String domain = "prod.anishkapuskar.me";



    public Object handleRequest(SNSEvent request, Context context) {



        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        context.getLogger().log("Invocation started: " + timeStamp);

        context.getLogger().log("1: " + (request == null));

        context.getLogger().log("2: " + (request.getRecords().size()));

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            context.getLogger().log("trying to connect to dynamodb");
            init();
            long unixTime = Instant.now().getEpochSecond()+20*60;
            Table table = dynamoDB.getTable("csye6225");
            if(table == null)
                context.getLogger().log("table is culprit");
            context.getLogger().log("Above Item");
            //context.getLogger().log("Table Description: "+table.getDescription().getTableName());
            Item item = table.getItem("username", request.getRecords().get(0).getSNS().getMessage());
            context.getLogger().log("Below Item");

            if(item==null) {
                Item itemPut = new Item()
                        .withPrimaryKey("username", request.getRecords().get(0).getSNS().getMessage())//string id
                        .withString("token", context.getAwsRequestId())
                        .withNumber("passwordTokenExpiry", unixTime);

                table.putItem(itemPut);

                try {
                    String FROM = "password_reset@"+domain;
                    String TO = request.getRecords().get(0).getSNS().getMessage();
                    String token = request.getRecords().get(0).getSNS().getMessageId();
                    AmazonSimpleEmailService client =
                            AmazonSimpleEmailServiceClientBuilder.standard()
                                    .withRegion(Regions.US_EAST_1).build();
                    SendEmailRequest req = new SendEmailRequest()
                            .withDestination(
                                    new Destination()
                                            .withToAddresses(TO))
                            .withMessage(
                                    new Message()
                                            .withBody(
                                                    new Body()
                                                            .withHtml(
                                                                    new Content()
                                                                            .withCharset(
                                                                                    "UTF-8")
                                                                            .withData(
                                                                                    "Please click on the below link to reset the password<br/>"+
                                                                                            "<p><a href='#'>https://"+domain+"/reset?email="+TO+"&token="+token+"</a></p>"))
                                            )
                                            .withSubject(
                                                    new Content().withCharset("UTF-8")
                                                            .withData("Password Reset Link")))
                            .withSource(FROM);
                    SendEmailResult response = client.sendEmail(req);
                    context.getLogger().log ("Email sent!");
                } catch (Exception ex) {
                    context.getLogger().log ("The email was not sent. Error message: "
                            + ex.getMessage());
                }
            }else{
                context.getLogger().log("-----------------------Item with id "+request.getRecords().get(0).getSNS().getMessage()+" already exist-----------------------");
                context.getLogger().log("Item with id "+request.getRecords().get(0).getSNS().getMessage()+" already exist");
            }
        }

        catch(Exception e){
            context.getLogger().log(e.getMessage());
        }

        context.getLogger().log("Invocation completed: " + timeStamp);

        return null;
    }

    private static void init() throws Exception {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        dynamoDB = new DynamoDB(client);
    }


}
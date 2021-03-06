import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONObject;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class lambdaapp implements RequestHandler<SNSEvent, Object> {

    static Context context = null;
    static LambdaLogger logger = null;

    public Object handleRequest(SNSEvent request, Context context) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        lambdaapp.context = context;
        lambdaapp.logger = context.getLogger();
        context.getLogger().log("Invocation started: " + timeStamp);

        String domain="prod.anishkapuskar.me";

        String snsemailjson = "{ \"username\":\""+request.getRecords().get(0).getSNS().getMessage()+"\"}";
        JSONObject body = new JSONObject(snsemailjson);

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
        dynamodb dbop = new dynamodb();
        String token = dbop.getToken(body.getString("username"));
        if(token!=null) {
            sendEmail(body.getString("username"), domain, token);
        }
        else{
            context.getLogger().log("Null token, email not sent");
        }
        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);
        return null;
    }

    public void sendEmail(String TO, String domain, String token) {

        String reset_url="http://"+domain+"/reset?email="+TO+"&token="+token;
        String FROM = "password_reset@" + domain;

        // The subject line for the email.
        String SUBJECT = "Password Reset";

        // The HTML body for the email.
        String HTMLBODY = "<h2>Password Reset</h2>"
                + "<p>Click <a href='"+reset_url+"'>"
                + "here</a> to reset you password or, copy and paste the below url in your browser <br><br>"
                + "<a href='"+reset_url+"'>" + reset_url+"</a>"
                + "<br><p>This link will expire in "+ 15+" minute(s)";

        // The email body for recipients with non-HTML email clients.
        String TEXTBODY = "To reset your password copy and paste the following url in your browser:\n\n "
                + reset_url+". This link will expire in "+ 15+" minute(s)";

        try {
            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1).withCredentials(new DefaultAWSCredentialsProviderChain()).build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination()
                            .withToAddresses(TO))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content().withCharset("UTF-8")
                                            .withData(HTMLBODY))
                                    .withText(new Content().withCharset("UTF-8")
                                            .withData(TEXTBODY)))
                            .withSubject(new Content().withCharset("UTF-8")
                                    .withData(SUBJECT)))
                    .withSource(FROM);

            client.sendEmail(request);
            context.getLogger().log("Password reset email has been sent to " + TO);
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: " + ex.getMessage());
        }
    }

}
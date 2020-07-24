import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.UUID;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import com.amazonaws.auth.AWSCredentialsProvider;

public class dynamodb {

    AmazonDynamoDB dynamoDBclient;
    DynamoDB dynamoDB = null;
    Table table;
    long TTL_MINS;
    dynamodb(){
        init();
    }
    private void init() {
        try {
            String dynamoDBEndPoint = System.getenv("DynamoDBEndPoint");
            dynamoDBclient = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(dynamoDBEndPoint, "us-east-1"))
                    .withCredentials(new DefaultAWSCredentialsProviderChain()).build();
            dynamoDB = new DynamoDB(dynamoDBclient);
            table = dynamoDB.getTable("csye6225");
            TTL_MINS=Long.valueOf(System.getenv("TTL_MINS"));
            lambdaapp.logger.log("Connection to DynamoDB successfull");
        } catch (Exception e) {
            lambdaapp.logger.log("Error in Constructor: " + e.getMessage());
        }
    }

    public void insert(String email, String token) {
        try {

            PutItemOutcome outcome = table.putItem(new Item().withPrimaryKey("email", email)
                    .withString("token", token)
                    .withNumber("expiration", getExpiryEpoch()));
            lambdaapp.logger.log("PutItem succeeded:\n" + outcome.toString());

        } catch (Exception e) {

            lambdaapp.logger.log("Insertion failed for: " + email);
            lambdaapp.logger.log("Error in Insert: " + e.getMessage());
        }

    }

    public String getToken(String email) {
        String token=null;
        try {
            lambdaapp.logger.log("Attempting to read the item...");

            QuerySpec spec = new QuerySpec()
                    .withKeyConditionExpression("email = :v_email")
                    .withFilterExpression("expiration > :v_expiration")
                    .withValueMap(new ValueMap()
                            .withString(":v_email", email)
                            .withNumber(":v_expiration", getCurrentEpoch()));

            ItemCollection<QueryOutcome> items = table.query(spec);
            Iterator<Item> iterator = items.iterator();
            while (iterator.hasNext()) {
                lambdaapp.logger.log(iterator.next().toJSONPretty());
            }

            if(items.getAccumulatedItemCount()==0){
                token=UUID.randomUUID().toString();
                insert(email, token);
                lambdaapp.logger.log("New token generated ");
            }
            else {
                lambdaapp.logger.log("Token retrieved from DB");
                token=null;
            }
            lambdaapp.logger.log("End of getToken");

        } catch (Exception e) {
            lambdaapp.logger.log("Unable to read item: " + email);
            lambdaapp.logger.log("Error in getToken: "+ e.getMessage());
        }
        return token;

    }

    private long getExpiryEpoch() {

        Instant now = Instant.now();
        Instant future = now.plus(TTL_MINS, ChronoUnit.MINUTES);
        long ttl = future.getEpochSecond();
        return ttl;
    }

    private long getCurrentEpoch() {
        Instant now = Instant.now();
        long ttl = now.getEpochSecond();
        return ttl;
    }

}
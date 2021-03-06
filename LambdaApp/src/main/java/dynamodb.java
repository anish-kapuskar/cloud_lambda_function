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
            String dynamoDBEndPoint = "dynamodb.us-east-1.amazonaws.com";
            dynamoDBclient = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(dynamoDBEndPoint, "us-east-1"))
                    .withCredentials(new DefaultAWSCredentialsProviderChain()).build();
            dynamoDB = new DynamoDB(dynamoDBclient);
            table = dynamoDB.getTable("csye6225");
            TTL_MINS=15;
            lambdaapp.logger.log("Connection to DynamoDB successfull");
        } catch (Exception e) {
            lambdaapp.logger.log("Error in Constructor: " + e.getMessage());
        }
    }

    public void insert(String username, String token) {
        try {

            PutItemOutcome outcome = table.putItem(new Item().withPrimaryKey("username", username)
                    .withString("token", token)
                    .withNumber("expiration", getExpiryEpoch()));
            lambdaapp.logger.log("PutItem succeeded:\n" + outcome.toString());

        } catch (Exception e) {

            lambdaapp.logger.log("Insertion failed for: " + username);
            lambdaapp.logger.log("Error in Insert: " + e.getMessage());
        }

    }

    public String getToken(String username) {
        String token=null;
        try {
            lambdaapp.logger.log("Attempting to read the item...");

            QuerySpec spec = new QuerySpec()
                    .withKeyConditionExpression("username = :v_username")
                    .withFilterExpression("expiration > :v_expiration")
                    .withValueMap(new ValueMap()
                            .withString(":v_username", username)
                            .withNumber(":v_expiration", getCurrentEpoch()));

            ItemCollection<QueryOutcome> items = table.query(spec);
            Iterator<Item> iterator = items.iterator();
            while (iterator.hasNext()) {
                lambdaapp.logger.log(iterator.next().toJSONPretty());
            }

            if(items.getAccumulatedItemCount()==0){
                token=UUID.randomUUID().toString();
                insert(username, token);
                lambdaapp.logger.log("New token generated ");
            }
            else {
                lambdaapp.logger.log("Token retrieved from DB");
                token=null;
            }
            lambdaapp.logger.log("End of getToken");

        } catch (Exception e) {
            lambdaapp.logger.log("Unable to read item: " + username);
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
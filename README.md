# Food Delivery Backend
This is a real time simulator of a food delivery order systems.

# Run
 
#### Pre-requisites
- Java JDK 11 or higher is installed https://adoptopenjdsk.net/installation.html
- Test the installation with this command ```java -version```

### [Unix systems or Windows Git Bash terminal]
The system has two components a server and an order generator:

1. Build the jar files run the command (This will build both systems):

    ```./mvnw clean package > build-result.orig && less -12 build-result.orig```
    
    **Note:** The output is directed to a file to improve execution

2. Start the server
    ```java -jar orderserver/target/orderserver-1.0.jar  <SERVER_OPTIONAL_ARGS>```

3. Start the order generator system (see the args table)
    ```java -jar orderserver/target/orderserver-1.0.jar <GENERATOR_OPTIONAL_ARGS>```
    
    #### Generator Optional Arguments
    
    |Name|Description|Values|
    |---|---|---|
    |order_source|the source of the orders to load in the system| *tiny*, *small*, *large* or your_own_file|
    |---|---|---|
    
    * **tiny**: points to default 5 order file within the system
    * **small**: points to default 24 order file within the system
    * **large**: points to default 129600 order file, equivalent to 24hrs (assuming 2 orders dispatched per second.
    * **your_own_file**: provide a path of your json file containing an array of Orders e.g. */mypath/to/file/orders.json*
    
    e.g.
    ```java -jar orderserver/target/orderserver-1.0.jar /mypath/to/file/orders.json```
    
### [Windows (CMD)]:
Replace the command ```./mvnw``` for ```mvnw.cmd```.


# Considerations

### Couriers
- The system loads a total of 10 couriers available.
- After dispatch the courier transits to "dispatched" state.
- After a courier delivers an order it transits back to "available".

### Orders
- This system dispatches the given orders at a rate of 2 orders per second.
- The orders are received on a queue and can be discarded if no courier is available for pick up.
- The matching between orders and couriers is done by the relevant strategy either FIFO or courier-order.
    
    ###### FIFO
    First Courier Arrives to kitchen picks the first available order
    
    ###### 


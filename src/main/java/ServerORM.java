
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import model.Articles;
import model.Clients;
import model.OrderLines;
import model.Orders;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.criterion.Order;
import org.json.*;

import java.sql.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * INSY Webshop Server
 */
public class ServerORM {

    /**
     * Port to bind to for HTTP service
     */
    private int port = 8000;
    SessionFactory sessionFactory;
    boolean firstStartUp = true;


    /**
     * Connect to the database
     * @throws IOException
     */
    Session setupDB() {
        //TODO Create hibernate.cfg.xml file with database properties
        //TODO Create SessionFactory on the first call of this method ONLY, return Session on EACH call.


        if( firstStartUp ) { // wird nur beim starten auseführt, damit die Session konfiguriert wird

            // A SessionFactory is set up once for an application!
            final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .configure() // configures settings from hibernate.cfg.xml
                    .build();

            try {

                sessionFactory = new MetadataSources( registry ).buildMetadata().buildSessionFactory();

            } catch( Exception e ) {

                // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
                // so destroy it manually.
                StandardServiceRegistryBuilder.destroy( registry );

            }

            firstStartUp = false;

        }

        return sessionFactory.openSession(); //öffnet die Session (Vebindung zur DB)

    }

    /**
     * Startup the Webserver
     * @throws IOException
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create( new InetSocketAddress( port ), 0 );
        server.createContext( "/articles", new ArticlesHandler() );
        server.createContext( "/clients", new ClientsHandler() );
        server.createContext( "/placeOrder", new PlaceOrderHandler() );
        server.createContext( "/orders", new OrdersHandler() );
        server.createContext( "/", new IndexHandler() );

        server.start();
    }


    public static void main( String[] args ) throws Throwable {
        ServerORM webshop = new ServerORM();
        webshop.start();
        System.out.println( "Webshop running at http://127.0.0.1:" + webshop.port );
    }


    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle( HttpExchange t ) throws IOException {
            Session ses = setupDB();

            JSONArray res = new JSONArray();

        /*
            //TODO read all articles and add them to res
            JSONObject art1 = new JSONObject();
            art1.put("id", 1);
            art1.put("description", "Bleistift");
            art1.put("price", 0.70);
            art1.put("amount", 2);
            res.put(art1);
            JSONObject art2 = new JSONObject();
            art2.put("id", 2);
            art2.put("description", "Papier");
            art2.put("price", 2);
            art2.put("amount", 100);
            res.put(art2);
        */

            /*
            sollte irgendein Fehler auftreten, so wird der ganze
            Nachstehende nicht Code ausgeführt. Es gibt keine halben Dinge,
            es wird nie nur eine Hälfte ausgeführt
             */
            ses.beginTransaction();


            List<Articles> articleList = ses.createQuery( "FROM Articles ", Articles.class ).list(); //nutzt das mapping File

            for( Articles article : articleList ) { // hat durch die DB alle Werte

                final JSONObject art = new JSONObject();
                art.put( "id", article.getId() );
                art.put( "description", article.getDescription() );
                art.put( "price", article.getPrice() );
                art.put( "amount", article.getAmount() );
                res.put( art );

            }

            ses.getTransaction().commit();
            ses.close();

            answerRequest( t, res.toString() );
        }

    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Override
        public void handle( HttpExchange t ) throws IOException {
            Session ses = setupDB();

            JSONArray res = new JSONArray();

            ses.beginTransaction();

            List<Clients> clientsList = ses.createQuery( "FROM Clients ", Clients.class ).list();

            for( Clients client : clientsList ) {

                final JSONObject cli = new JSONObject();

                cli.put( "id", client.getId() );
                cli.put( "name", client.getName() );
                cli.put( "address", client.getAddress() );
                cli.put( "city", client.getCity() );
                cli.put( "country", client.getCountry() );

                res.put( cli );

            }

            ses.getTransaction().commit();
            ses.close();
        /*
            //TODO read all clients and add them to res
    	    JSONObject cli = new JSONObject();
            cli.put("id", 1);
            cli.put("name", "Brein");
            cli.put("address", "TGM, 1220 Wien");
            res.put(cli);

        */
            answerRequest( t, res.toString() );
        }

    }


    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Override
        public void handle( HttpExchange t ) throws IOException {
            Session ses = setupDB();

            JSONArray res = new JSONArray();

        /*
            //TODO read all orders and add them to res
            // Join orders with clients, order lines, and articles
            // Get the order id, client name, number of lines, and total prize of each order and add them to res
            JSONObject ord = new JSONObject();
	        ord.put("id", 1);
            ord.put("client", "Brein");
            ord.put("lines", 2);
            ord.put("price", 3.5);
            res.put(ord);


        */

            ses.beginTransaction();
            List<Orders> orderList = ses.createQuery( "FROM Orders ", Orders.class ).list();

            for( Orders order : orderList ) {

                double price = 0;
                for( OrderLines orderLine : order.getOrderLines() ) {

                    price += orderLine.getAmount() * orderLine.getArticle().getPrice();

                }

                JSONObject ord = new JSONObject();

                ord.put( "id", order.getId() );
                ord.put( "client", order.getClient().getName() );
                ord.put( "lines", order.getOrderLines().size() );
                ord.put( "price", price );

                res.put( ord );

            }

            ses.getTransaction().commit();
            ses.close();

            answerRequest( t, res.toString() );

        }
    }


    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle( HttpExchange t ) throws IOException {

            Session ses = setupDB();
            Map<String, String> params = queryToMap( t.getRequestURI().getQuery() );

            int client_id = Integer.parseInt( params.get( "client_id" ) );

            String response = "";
            int order_id = 1;
            try {

                ses.beginTransaction();

                //TO DO Get the next free order id

                order_id = ses.createQuery( "FROM Orders", Orders.class )
                        .list()
                        .stream()
                        .map( Orders::getId )
                        .max( Integer::compareTo )
                        .orElseThrow() + 1;


                //TO DO Create a new order with this id for client client_id
                Clients client = ( Clients ) ses.createQuery( "FROM Clients C WHERE C.id = " + client_id)
                        .getSingleResult();

                Orders newOrder = new Orders();
                newOrder.setId( order_id );
                newOrder.setCreated_at( new Timestamp( System.currentTimeMillis() ) );
                Clients workingClient = new Clients();
                workingClient.setAddress( client.getAddress() );
                workingClient.setCity( client.getCity() );
                workingClient.setId( client.getId() );
                workingClient.setCountry( client.getCountry() );
                workingClient.setName( client.getName() );
                newOrder.setClient( workingClient );

                ses.save( newOrder );

                for( int i = 1; i <= ( params.size() - 1 ) / 2; ++i ) {
                    int article_id = Integer.parseInt( params.get( "article_id_" + i ) );
                    int amount = Integer.parseInt( params.get( "amount_" + i ) );


                    //TO DO Get the available amount for article article_id
                    Articles art = ses.createQuery( "FROM Articles WHERE Articles.id = " + article_id, Articles.class )
                            .list()
                            .get( 0 );

                    if( art.getAmount() < amount ) {

                        throw new IllegalArgumentException( String.format( "Not enough items of article #%d available", article_id ) );

                    }

                    //TO DO Decrease the available amount for article article_id by amount
                    art.setAmount( art.getAmount() - amount );
                    ses.update( art );

                    //TO DO Insert new order line
                    int orderLines_id = ses.createQuery( "FROM OrderLines", Orders.class )
                            .list()
                            .stream()
                            .map( Orders::getId )
                            .max( Integer::compareTo )
                            .orElseThrow() + 1;

                    OrderLines newOrderLines = new OrderLines();
                    newOrderLines.setId( orderLines_id );
                    newOrderLines.setOrder( newOrder );
                    newOrderLines.setArticle( art );
                    newOrderLines.setAmount( amount );

                    ses.save( newOrderLines );

                }

                response = String.format( "{\"order_id\": %d}", order_id );
            } catch( IllegalArgumentException iae ) {
                response = String.format( "{\"error\":\"%s\"}", iae.getMessage() );
            }

            ses.getTransaction().commit();
            ses.close();

            answerRequest( t, response );


        }
    }

    /**
     * Handler for listing static index page
     */
    class IndexHandler implements HttpHandler {
        @Override
        public void handle( HttpExchange t ) throws IOException {
            String response = "<!doctype html>\n" +
                    "<html><head><title>INSY Webshop</title><link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/water.css@2/out/water.css\"></head>" +
                    "<body><h1>INSY Pseudo-Webshop</h1>" +
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>" +
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/articles\">http://127.0.0.1:" + port + "/articles</a></dd>" +
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/orders\">http://127.0.0.1:" + port + "/orders</a></dd>" +
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/clients\">http://127.0.0.1:" + port + "/clients</a></dd>" +
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:" + port + "/placeOrder?client_id=<client_id>&article_id_1=<article_id_1>&amount_1=<amount_1&article_id_2=<article_id_2>&amount_2=<amount_2>\">http://127.0.0.1:" + port + "/placeOrder?client_id=&lt;client_id>&article_id_1=&lt;article_id_1>&amount_1=&lt;amount_1>&article_id_2=&lt;article_id_2>&amount_2=&lt;amount_2></a></dd>" +
                    "</dl></body></html>";

            answerRequest( t, response , false);
        }

    }

    private void answerRequest( HttpExchange t, String response ) throws IOException {
        this.answerRequest( t, response, true );
    }

    /**
     * Helper function to send an answer given as a String back to the browser
     * @param t        HttpExchange of the request
     * @param response Answer to send
     * @throws IOException
     */
    private void answerRequest( HttpExchange t, String response, boolean json ) throws IOException {
        byte[] payload = response.getBytes();
        if( json ) {
            t.getResponseHeaders().set( "Content-Type", "application/json" );
        }
        t.sendResponseHeaders( 200, payload.length );
        OutputStream os = t.getResponseBody();
        os.write( payload );
        os.close();
    }

    /**
     * Helper method to parse query paramaters
     * @param query
     * @return
     */
    public static Map<String, String> queryToMap( String query ) {
        Map<String, String> result = new HashMap<String, String>();
        for( String param : query.split( "&" ) ) {
            String pair[] = param.split( "=" );
            if( pair.length > 1 ) {
                result.put( pair[ 0 ], pair[ 1 ] );
            } else {
                result.put( pair[ 0 ], "" );
            }
        }
        return result;
    }


}

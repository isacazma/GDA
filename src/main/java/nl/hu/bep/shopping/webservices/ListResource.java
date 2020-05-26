package nl.hu.bep.shopping.webservices;

import nl.hu.bep.shopping.model.Product;
import nl.hu.bep.shopping.model.Shop;
import nl.hu.bep.shopping.model.Shopper;
import nl.hu.bep.shopping.model.ShoppingList;

import javax.json.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;

@Path("list")
public class ListResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShoppingLists() {
        if (Shop.getShop().getAllShoppingLists().isEmpty())
            return Response.noContent().build(); // 204: succesvol verzoek, maar niets om te tonen
        ArrayList<HashMap<String,String>> info = new ArrayList<>();
        for(ShoppingList sl : Shop.getShop().getAllShoppingLists()){
            HashMap<String,String> lijst = new HashMap<>();
            lijst.put("listname", sl.getName());
            lijst.put("owner", sl.getOwner().getName());
            info.add(lijst);
        }
        return Response.ok(info).build(); //op deze manier houden we jackson binnen de perken zonder @JsonIgnore
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShoppingListByName(@PathParam("name") String name) {
        if (Shop.getShop().getShoppingListByName(name) == null) return Response.noContent().build();
        return Response.ok(Shop.getShop().getShoppingListByName(name)).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShoppingList(@FormParam("owner") String owner, @FormParam("name") String listname) {
        //Dit voorbeeld zo expliciet mogelijk uitgewerkt, bevat vele checks, maar geeft ENKEL statuscodes terug en gebruikt FormParam als input
        Shopper found = Shopper.getAllShoppers().stream().filter(e->e.getName().equals(owner)).findFirst().orElse(null);
        if (found != null) { //werkt want equals checkt enkel op name van shopper
            if (found.addList(new ShoppingList(listname, found))) { //addList laat ons weten of het toevoegen gelukt is (ivm dubbele namen kan dit mislukken)
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.CONFLICT).build(); //er bestond al zo'n lijst waarschijnlijk
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build(); //er bestond geen shopper met die naam
        }
    }


    @POST
    @Path("/item") //om deze post te onderscheiden van de POST hierboven
    @Produces(MediaType.APPLICATION_JSON)
    public Response addProductToList(String jsonBody) {
        //Dit voorbeeld gebruikt json als input en geeft bij de statuscode ook meldingen mee met daarin de fout, dat is wat prettiger voor de frontend programmeur
        //JsonObjectBuilder responseObject = Json.createObjectBuilder();
        try {
            StringReader strReader = new StringReader(jsonBody);      //deze drie regels
            JsonReader jsonReader = Json.createReader(strReader);     //laten we expres apart
            JsonStructure jsonStructure = jsonReader.read();          //zodat foutopsporing duidelijk blijft

            if (jsonStructure.getValueType() == JsonValue.ValueType.OBJECT) { //valideer dat het een object bevat
                JsonObject productObject = (JsonObject) jsonStructure;
                String shoppingListName = productObject.getString("list");
                String productName = productObject.getString("productname");
                int amount = Integer.parseInt(productObject.getString("amount"));
                ShoppingList theList = ShoppingList.getAllLists().stream().filter(e->e.getName().equals(shoppingListName)).findFirst().orElse(null);
                if(theList==null){
                    return Response.status(Response.Status.NOT_FOUND).entity(new SimpleEntry<>("error", "could not find list: "+shoppingListName)).build();
                }
                if(theList.addItem(new Product(productName), amount)) {
                    return Response.ok().build();
                }
                return Response.status(Response.Status.CONFLICT).entity(new SimpleEntry<>("error", "could not add product due to restrictions")).build();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(new SimpleEntry<>("error", "no valid JsonValue given, was type: "+jsonStructure.getValueType().toString())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).entity(new SimpleEntry<>("error", e.getMessage())).build();
        }
    }
}

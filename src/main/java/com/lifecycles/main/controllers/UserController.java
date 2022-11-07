package com.lifecycles.main.controllers;
// package com.lifecycles.main.Controllers;

// import java.util.List;

// import javax.servlet.http.HttpServletRequest;

// import org.bson.BsonValue;
// import org.bson.Document;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestMethod;
// import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.bind.annotation.RestController;

// import com.lifecycles.main.Models.User;
// import com.lifecycles.main.Repositories.UserRepository;
// import com.lifecycles.main.Services.UserService;
// import com.mongodb.MongoException;
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoCollection;

// @RestController
// @RequestMapping(value = "/users")
// public class UserController {

  
//   @Autowired
//   private UserService userService;


//   @GetMapping(value = "/getall")
//   public List<User> findAll() {
//     return userService.findAll();
//   }

//   @GetMapping(value = "/getbyfirstname/{firstName}")
//   public User findByFirstName(@PathVariable String firstName) {
//     return userService.findByFirstName(firstName);
//   }

//   @GetMapping(value = "/getbylastname/{lastName}")
//   public List<User> findByLastName(@PathVariable String lastName) {
//     return userService.findByLastName(lastName);
//   }

//   @GetMapping(value = "/getbyusername/{username}")
//   public User findByUsername(@PathVariable String username) {
//     return userService.findByUsername(username);
//   }

//   @GetMapping(value = "/getbyemail/{email}")
//   public User findByEmail(@PathVariable String email) {
//     return userService.findByEmail(email);
//   }

//   @GetMapping(value = "/getbyphonenumber/{phoneNumber}")
//   public User findByPhoneNumber(@PathVariable String phoneNumber) {
//     return userService.findByPhoneNumber(phoneNumber);
//   }

//   @RequestMapping(value = "/create", method = RequestMethod.POST)
//   @ResponseBody
//   public User create(HttpServletRequest request) {
//     String firstName = request.getParameter("firstName");
//     String lastName = request.getParameter("lastName");
//     String username = request.getParameter("username");
//     String email = request.getParameter("email");
//     String phoneNumber = request.getParameter("phoneNumber");
//     String hashedPassword = request.getParameter("hashedPassword");
//     String normalPassword = request.getParameter("normalPassword");

//     User user = new User();
//     user.setFirstName(firstName);
//     user.setLastName(lastName);
//     user.setUsername(username);
//     user.setEmail(email);
//     user.setPhoneNumber(phoneNumber);
//     user.setHashedPassword(hashedPassword);
//     user.setNormalPassword(normalPassword);
//     Document customdatatypeTomongo = new Document("firstname", firstName)
//     .append("lastname", lastName).append("username", username).append("email", email)
//     .append("phoneNumber", phoneNumber).append("hashedPassword", hashedPassword)
//     .append("normalPassword", normalPassword);
    
//     try {
//       Document doc = new Document("firstName", "key").append("name", "mike");
//       BsonValue id = personcollection.insertOne(doc).getInsertedId();
//       System.out.println("inserted id: " + id);
//     } catch (MongoException me) {
//       System.err.println("error in insertone: " + me);
//     }
//     return userService.create(user);
//   }

//   @RequestMapping(value = "/edit", method = RequestMethod.PUT)
//   @ResponseBody
//   public User edit(HttpServletRequest request) {
//     String id = request.getParameter("id");
//     String firstName = request.getParameter("firstName");
//     String lastName = request.getParameter("lastName");
//     String username = request.getParameter("username");
//     String email = request.getParameter("email");
//     String phoneNumber = request.getParameter("phoneNumber");
//     String hashedPassword = request.getParameter("hashedPassword");
//     String normalPassword = request.getParameter("normalPassword");

//     User user = new User();
//     user.setId(id);
//     user.setFirstName(firstName);
//     user.setLastName(lastName);
//     user.setUsername(username);
//     user.setEmail(email);
//     user.setPhoneNumber(phoneNumber);
//     user.setHashedPassword(hashedPassword);
//     user.setNormalPassword(normalPassword);

//     return userService.edit(user);
//   }

//   @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
//   @ResponseBody
//   public void deleteById(@PathVariable String id) {
//     userService.deleteById(id);
//   }

// @RequestMapping(value = "hello", method = RequestMethod.GET)
// public String helloForm(){
//     String html = "<form method='post'>" +
//             "<input type='text' name='name' />" +
//             "<input type='submit' value='Greet Me!'/>" +
//             "</form>";
//     return html;
// }

// @RequestMapping(value = "hello", method = RequestMethod.POST)
// @ResponseBody
// public String index(HttpServletRequest request){
//   String name = request.getParameter("name");
//   return "Hello " + name;
// }

// @RequestMapping(value = "hello/{name}")
// @ResponseBody
// public String helloUrlSegment(@PathVariable String name){
//   return "Hello " + name;
// }

// }
package user;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.math.NumberUtils;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;


@RestController
public class UserController extends WebSecurityConfigurerAdapter {
    private GeodeticCalculator geoCalc = new GeodeticCalculator();
    private Ellipsoid reference = Ellipsoid.WGS84;

    @Autowired
    UserRepository userRepository;

    //    @Override
//    protected void configure (HttpSecurity http) throws Exception {
//        http.csrf().disable();
//    }

    @RequestMapping(method = RequestMethod.GET, value="/jrt/api/v1.0/users")
    public ResponseEntity<Collection<User>> users() {
        Collection<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return new ResponseEntity(users, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<Collection<User>>(users, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value="/jrt/api/v1.0/user/{userId}")
    public ResponseEntity<?> get_user(@PathVariable String userId) {
        if (!NumberUtils.isCreatable(userId)) {
            return new ResponseEntity(new ApplicationError("Invalid user id: " + userId
                    + "."), HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findUserByUserId(Integer.valueOf(userId));
        if (null == user) {
            return new ResponseEntity(new ApplicationError("User with id " + userId
                    + " not found"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value="/jrt/api/v1.0/user")
    public ResponseEntity<?> add_user(@Valid @RequestBody User input) {
        User userFound = userRepository.findUserByUserId(input.getUserId());
        if(userFound != null){
            return new ResponseEntity(new ApplicationError("Unable to create user. Record already exist"),HttpStatus.CONFLICT);
        }
        User user = userRepository.save(input);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(UriComponentsBuilder.fromPath("/jrt/api/v1.0/user/{userId}").buildAndExpand(user.getUserId()).toUri());
        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PUT, value="/jrt/api/v1.0/user/{userId}")
    public ResponseEntity<?> update_user(@PathVariable String userId, @RequestBody User input) {
        User user = userRepository.findUserByUserId(Integer.valueOf(userId));
        if (null == user) {
            return new ResponseEntity(new ApplicationError("User not found."),HttpStatus.NOT_FOUND);
        }
        if(input.getFirstName() != null){
            user.setFirstName(input.getFirstName());
        }
        if(input.getLastName() != null){
            user.setLastName(input.getLastName());
        }
        if(input.getLatitude() != null){
            user.setLatitude(input.getLatitude());
        }
        if(input.getLongitude() != null){
            user.setLongitude(input.getLongitude());
        }
        User updatedUser = userRepository.save(user);
        return new ResponseEntity(updatedUser, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/jrt/api/v1.0/user/{userId}")
    public ResponseEntity<?> delete_user(@PathVariable String userId) {
        User user = userRepository.findUserByUserId(Integer.valueOf(userId));
        if (null == user) {
            return new ResponseEntity(new ApplicationError("User not found."),HttpStatus.NOT_FOUND);
        }
        userRepository.delete(user);
        return new ResponseEntity(user, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/jrt/api/v1.0/user")
    public ResponseEntity<?> delete_all() {
        userRepository.deleteAll();
        return new ResponseEntity<User>(HttpStatus.OK);
    }


    /**
     * Distance verified manually using https://www.movable-type.co.uk/scripts/latlong.html.
     * Some profiling or performance tuning may be needed here based on the architecture of the computer,
     * number of cores, thread pool best suited for the use case we are running.
     */
    @RequestMapping(method = RequestMethod.GET, value="/jrt/api/v1.0/distances")
    public ResponseEntity<?> distances() {
        /**
         * Each user has a lat/lon associated with them.  Determine the distance
         * between each user pair, and provide the min/max/average/std as a json response.
         * This should be GET only.
         *
         */
        // In a real case scenario you dont really map everything against everything, maybe the search can be narrowed per country,
        // or per block and so on. Then we can optimize this call, instead of find all.
        Set<User> set = new HashSet<User>(userRepository.findAll());

        Map<String, Double> mapOfIdsAndDistances = Sets.combinations(set, 2).parallelStream().map(s -> {
            List<User> list = new ArrayList<User>(s);
            User userA = list.get(0);
            User userB = list.get(1);
            GlobalPosition pointA = new GlobalPosition(userA.getLatitude(), userA.getLongitude(), 0.0);
            GlobalPosition pointB = new GlobalPosition(userB.getLatitude(), userB.getLongitude(), 0.0);
            double distance = geoCalc.calculateGeodeticCurve(reference, pointB, pointA).getEllipsoidalDistance();

            StringBuilder sb = new StringBuilder();
            sb.append(userA.getUserId()).append("-").append(userB.getUserId());

            return new PairOfIdsToDistance(sb.toString(), distance);
        }).collect(Collectors.toMap(PairOfIdsToDistance::getFromToIds, PairOfIdsToDistance::getDistance));

        DoubleStatistics stats = mapOfIdsAndDistances.values().stream().collect(DoubleStatistics.collector());
        return new ResponseEntity<DoubleStatistics>(stats, HttpStatus.OK);
    }

}

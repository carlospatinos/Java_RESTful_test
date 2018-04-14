package user;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    private User carlos, pedro, luis;

    @Before
    public void setup(){
        repository.deleteAll();
        carlos = repository.save(new User(1, "Carlos", "Patino", 53.4239330, -7.9406900));
        pedro = repository.save(new User(2, "Pedro", "Rodriguez", 53.4239330, -7.9406900));
        luis = repository.save(new User(3, "Luis", "Garcia", 53.4239330, -7.9406900));
    }

    @Test
    public void setsIdOnSave() {
        User david = repository.save(new User("Dave", "Matthews", 53.4239330, -7.9406900));
        assertNotNull(david.getUserId());
    }

    @Test
    public void findByUserId() {
        User david = repository.save(new User(4, "David", "Martinez", 53.4239330, -7.9406900));
        User founded = repository.findUserByUserId(david.getUserId());
        assertEquals(david.getFirstName(), founded.getFirstName());
    }
}
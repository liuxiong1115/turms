package im.turms.turms.cluster;

import com.hazelcast.cluster.Member;
import im.turms.turms.property.TurmsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static im.turms.turms.cluster.TurmsClusterManager.HASH_SLOTS_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * The tests require running with at lease one other turms server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TurmsClusterManagerST {
    @LocalServerPort
    Integer port;

    @Autowired
    private TurmsClusterManager turmsClusterManager;

    @Test
    public void getMembersSize_shouldReturnNumberMoreThanOne() {
        int size = turmsClusterManager.getMembers().size();
        assertTrue(size > 1, "The tests require running with at lease one other turms server");
    }

    @Test
    public void updatePropertiesAndNotify_shouldSucceed() {
        Function<TurmsProperties, Void> callback = mock(Function.class);
        TurmsProperties.addListeners(callback);
        TurmsProperties properties = new TurmsProperties();
        int number = 123456789;
        properties.getMessage().setMaxRecordsSizeBytes(number);
        properties.getGroup().setGroupInvitationContentLimit(number);

        turmsClusterManager.updatePropertiesAndNotify(properties);

        turmsClusterManager.fetchSharedPropertiesFromCluster();
        TurmsProperties currentProperties = turmsClusterManager.getTurmsProperties();

        Mockito.verify(callback, Mockito.times(1)).apply(any());
        assertEquals(number, currentProperties.getMessage().getMaxRecordsSizeBytes());
        assertEquals(number, currentProperties.getGroup().getGroupInvitationContentLimit());
    }

    @Test
    public void getMemberByUserId_shouldSucceed() {
        List<Member> members = new ArrayList<>(turmsClusterManager.getMembers());
        int size = members.size();
        if (size <= 1) {
            throw new RuntimeException("The tests require running with at lease one other turms server");
        }
        for (long userId = 0; userId < 100; userId++) {
            long slot = userId % HASH_SLOTS_NUMBER;
            Member member = turmsClusterManager.getMemberByUserId(userId);
            int memberIndex = members.indexOf(member);
            if (memberIndex != -1) {
                int step = HASH_SLOTS_NUMBER / size;
                int start = memberIndex * step;
                int end = (memberIndex + 1) * step;
                if (slot < start || size >= end) {
                    throw new RuntimeException("Turms member doesn't join the cluster");
                }
            } else {
                throw new RuntimeException("An unknown member is found");
            }
        }
        assertTrue(true);
    }
}

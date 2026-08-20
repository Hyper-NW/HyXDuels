package me.alphatct3209.duels.party;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartyTest
{
    @Test
    void rolesPromoteDemoteTransferAndVisibilityPreserveOneLeader()
    {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(leader);
        party.add(member);

        assertEquals(PartyRole.LEADER, party.role(leader));
        assertEquals(PartyRole.MEMBER, party.role(member));
        party.promote(member);
        assertEquals(PartyRole.MODERATOR, party.role(member));
        party.demote(member);
        party.transfer(member);

        assertEquals(member, party.leader());
        assertEquals(PartyRole.LEADER, party.role(member));
        assertEquals(PartyRole.MODERATOR, party.role(leader));
        assertTrue(party.visible());
        assertFalse(party.toggleVisible());
    }

    @Test
    void leaderCannotBeRemovedAndInvalidRoleTransitionsFail()
    {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(leader);
        party.add(member);

        assertThrows(IllegalStateException.class, () -> party.remove(leader));
        assertThrows(IllegalStateException.class, () -> party.demote(member));
        party.promote(member);
        assertThrows(IllegalStateException.class, () -> party.promote(member));
    }
}

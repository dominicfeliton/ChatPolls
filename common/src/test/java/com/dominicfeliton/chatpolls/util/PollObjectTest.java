package com.dominicfeliton.chatpolls.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PollObjectTest {
    private PollObject poll;
    private TestClock clock;
    private final String TITLE = "Test Poll";
    private final String DESCRIPTION = "Test Description";
    private final List<String> OPTIONS = Arrays.asList("Option 1", "Option 2", "Option 3");
    private final long DELAY_SEC = 5;
    private final long DURATION_SEC = 60;

    private static class TestClock implements Clock {
        private LocalDateTime currentTime = LocalDateTime.now();

        public void setCurrentTime(LocalDateTime time) {
            this.currentTime = time;
        }

        @Override
        public LocalDateTime getCurrentDateTime() {
            return currentTime;
        }
    }

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        poll = new PollObject(TITLE, OPTIONS, DESCRIPTION, DELAY_SEC, DURATION_SEC, clock) {};
    }

    @Test
    void testBasicProperties() {
        assertEquals(TITLE, poll.getTitle());
        assertEquals(DESCRIPTION, poll.getDescription());
        assertEquals(OPTIONS, poll.getOptions());
        assertEquals("Option 1, Option 2, Option 3", poll.getOptionsDisplay());
    }

    @Test
    void testVoteTracking() {
        UUID voter = UUID.randomUUID();
        
        // Test initial state
        assertFalse(poll.hasVoted(voter));
        assertNull(poll.getPlayerVote(voter));
        
        // Move time to during poll
        clock.setCurrentTime(poll.getCurrentDateTime().plusSeconds(DELAY_SEC + 1));
        
        // Test successful vote
        assertTrue(poll.castVote(voter, "Option 1"));
        assertTrue(poll.hasVoted(voter));
        assertEquals("Option 1", poll.getPlayerVote(voter));
        assertEquals(1, poll.getOptionVotes().get("Option 1"));
        
        // Test duplicate vote
        assertFalse(poll.castVote(voter, "Option 2"));
        assertEquals("Option 1", poll.getPlayerVote(voter));
        assertEquals(1, poll.getOptionVotes().get("Option 1"));
        assertEquals(0, poll.getOptionVotes().get("Option 2"));
    }

    @Test
    void testTimeBasedOperations() {
        LocalDateTime startTime = poll.getCurrentDateTime().plusSeconds(DELAY_SEC);
        
        // Test before start
        assertFalse(poll.hasStarted());
        assertFalse(poll.hasEnded());
        
        // Test during poll
        clock.setCurrentTime(startTime.plusSeconds(1));
        assertTrue(poll.hasStarted());
        assertFalse(poll.hasEnded());
        
        // Test after end
        clock.setCurrentTime(startTime.plusSeconds(DURATION_SEC + 1));
        assertTrue(poll.hasStarted());
        assertTrue(poll.hasEnded());
    }

    @Test
    void testInvalidVotes() {
        UUID voter = UUID.randomUUID();
        
        // Test invalid option
        assertFalse(poll.castVote(voter, "Invalid Option"));
        
        // Test vote before start
        assertFalse(poll.castVote(voter, "Option 1"));
        
        // Test vote after end
        clock.setCurrentTime(poll.getCurrentDateTime().plusSeconds(DELAY_SEC + DURATION_SEC + 1));
        assertFalse(poll.castVote(voter, "Option 1"));
    }

    @Test
    void testRankedVoting() {
        UUID voter = UUID.randomUUID();
        poll.setPollType(PollType.RANKED);
        
        // Move time to during poll
        clock.setCurrentTime(poll.getCurrentDateTime().plusSeconds(DELAY_SEC + 1));
        
        // Test valid ranked vote
        List<String> ranking = Arrays.asList("Option 1", "Option 2", "Option 3");
        assertTrue(poll.castRankedVote(voter, ranking));
        assertEquals(ranking, poll.getRankedVotes(voter));
        
        // Test invalid rankings
        assertFalse(poll.castRankedVote(voter, Arrays.asList("Invalid", "Option 1")));
        assertFalse(poll.castRankedVote(voter, Arrays.asList("Option 1", "Option 1"))); // Duplicate
    }

    @Test
    void testRankedWinnerCalculation() {
        poll.setPollType(PollType.RANKED);
        clock.setCurrentTime(poll.getCurrentDateTime().plusSeconds(DELAY_SEC + 1));
        
        // Create test scenario with three voters
        UUID voter1 = UUID.randomUUID();
        UUID voter2 = UUID.randomUUID();
        UUID voter3 = UUID.randomUUID();
        
        // Voter 1: Option 1 > Option 2 > Option 3
        poll.castRankedVote(voter1, Arrays.asList("Option 1", "Option 2", "Option 3"));
        
        // Voter 2: Option 2 > Option 1 > Option 3
        poll.castRankedVote(voter2, Arrays.asList("Option 2", "Option 1", "Option 3"));
        
        // Voter 3: Option 2 > Option 3 > Option 1
        poll.castRankedVote(voter3, Arrays.asList("Option 2", "Option 3", "Option 1"));
        
        // Option 2 should win as it has majority support
        assertEquals("Option 2", poll.calculateRankedWinner());
    }
}

/*
 * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
 * Copyright (C) 2010  Mickael Guessant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package davmail.exchange.graph;

import davmail.exchange.AbstractExchangeSessionTestCase;

import java.io.IOException;
import java.util.UUID;

/**
 * Event reminders over Graph: https://github.com/mguessan/davmail/issues/543
 */
public class TestGraphExchangeSessionEventReminders extends AbstractExchangeSessionTestCase {

    protected String createAndGetBody(String itemName, String trigger) throws IOException {
        String itemBody = "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//davmail//test//EN\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:" + UUID.randomUUID() + "\r\n" +
                "DTSTAMP:20261015T120000Z\r\n" +
                "DTSTART:20261230T140000Z\r\n" +
                "DTEND:20261230T150000Z\r\n" +
                "SUMMARY:davmail reminder test\r\n" +
                "BEGIN:VALARM\r\n" +
                "ACTION:DISPLAY\r\n" +
                "DESCRIPTION:Reminder\r\n" +
                trigger + "\r\n" +
                "END:VALARM\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";
        session.createOrUpdateItem("calendar", itemName, itemBody, null, "*");
        return session.getItem("calendar", itemName).getBody();
    }

    public void testReminderAtStart() throws IOException {
        String itemName = UUID.randomUUID() + ".EML";
        try {
            String body = createAndGetBody(itemName, "TRIGGER:PT0S");
            // a VALARM without TRIGGER is rejected by clients
            assertTrue(body, body.contains("TRIGGER:-PT0M"));
        } finally {
            session.deleteItem("calendar", itemName);
        }
    }

    public void testReminderWithSeveralComponents() throws IOException {
        String itemName = UUID.randomUUID() + ".EML";
        try {
            String body = createAndGetBody(itemName, "TRIGGER:-PT1H30M");
            assertTrue(body, body.contains("TRIGGER:-PT90M"));
        } finally {
            session.deleteItem("calendar", itemName);
        }
    }
}

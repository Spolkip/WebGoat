/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.util.Base64;
import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({
  "insecure-deserialization.hints.1",
  "insecure-deserialization.hints.2",
  "insecure-deserialization.hints.3"
})
public class InsecureDeserializationTask implements AssignmentEndpoint {

private static class DeserializationFilter implements ObjectInputFilter {
    @Override
    public Status checkInput(FilterInfo filterInfo) {
        Class<?> clazz = filterInfo.serialClass();
        if (clazz != null) {
            // Only allow the exact class, not subclasses, arrays, or primitives
            if (clazz.getName().equals("org.dummy.insecure.framework.VulnerableTaskHolder")) {
                return Status.ALLOWED;
            }
            return Status.REJECTED;
        }
        return Status.UNDECIDED;
    }
}
    @PostMapping("/InsecureDeserialization/task")
    @ResponseBody
    public AttackResult completed(@RequestParam String token) throws IOException {
        String b64token;
        long before;
        long after;
        int delay;

        b64token = token.replace('-', '+').replace('_', '/');

        try (ObjectInputStream ois =
                new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64token)))) {
            ois.setObjectInputFilter(new DeserializationFilter());
            before = System.currentTimeMillis();
            Object o = ois.readObject();
            if (!(o instanceof VulnerableTaskHolder)) {
                if (o instanceof String) {
                    return failed(this).feedback("insecure-deserialization.stringobject").build();
                }
                return failed(this).feedback("insecure-deserialization.wrongobject").build();
            }
            after = System.currentTimeMillis();
        } catch (InvalidClassException e) {
            return failed(this).feedback("insecure-deserialization.invalidversion").build();
        } catch (IllegalArgumentException e) {
            return failed(this).feedback("insecure-deserialization.expired").build();
        } catch (Exception e) {
            return failed(this).feedback("insecure-deserialization.invalidversion").build();
        }

        delay = (int) (after - before);
        if (delay > 7000) {
            return failed(this).build();
        }
        if (delay < 3000) {
            return failed(this).build();
        }
        return success(this).build();
    }
}
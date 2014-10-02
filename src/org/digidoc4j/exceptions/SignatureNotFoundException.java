package org.digidoc4j.exceptions;

public class SignatureNotFoundException extends DigiDoc4JException {
  public SignatureNotFoundException() {
    super("Signature not found");
  }
}
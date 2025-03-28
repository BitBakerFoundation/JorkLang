package org.parser;

import org.lexer.Token;
import org.parser.expressions.*;

public class Identifier implements Expression {
  Token token;
  String value;

  Identifier(Token tok, String val) {
    this.token = tok;
    this.value = val;
  }

  @Override
  public String getTokenValue() {
    return this.token.getTokenValue();
  }

  @Override
  public void expressionNode() {
  }

  @Override
  public String getNodeValue() {
    return this.value;
  }

  @Override
  public String print(String msg) {
    return (msg + this.getNodeValue());
  }

  public String getValue() {
    return this.value;
  }
}

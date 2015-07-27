package org.sectorzero;

import org.apache.commons.lang3.StringUtils;

import java.util.Stack;
import java.util.StringTokenizer;

import static org.sectorzero.InfixEvaluation.EvalState.*;
import static org.sectorzero.InfixEvaluation.Operator.*;
import static org.sectorzero.InfixEvaluation.TokenType.*;

import org.apache.commons.lang3.Validate;

import lombok.Value;

public class InfixEvaluation {

  public int eval(String s) throws Exception {
    if(StringUtils.isEmpty(s)) {
      throw new Exception();
    }

    EvalMachine m = new EvalMachine();
    m.apply(new Token(StartExpr, NA, 0));

    StringTokenizer tokenizer = new StringTokenizer(s, "+-*/()", true);
    while(tokenizer.hasMoreTokens()) {
      String tokenStr = tokenizer.nextToken().trim();
      Token t = Token.fromTokenString(tokenStr);
      m.apply(t);
    }

    m.apply(new Token(EndExpr, NA, 0));
    if(m.isError()) {
      throw new Exception();
    }

    return m.last();
  }

  static class EvalMachine {
    Stack<EvalNode> s = new Stack<>();
    EvalState state = Start;

    public void apply(Token t) {
      switch (state) {
        case Start:
          if(t.getType() == Operand) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = Terminal;
          } else if(t.getType() == StartExpr) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_1;
          } else {
            state = Error;
          }
          break;

        case WaitingForUnaryOperand:
          if(t.getType() == StartExpr) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_1;
          } else if(t.getType() == Operand) {
            EvalNode last = s.pop();
            int value = eval(last.getToken().getOperator(), t.getOperand());
            state = last.previousState;
            apply(new Token(Operand, NA, value));
          } else {
            state = Error;
          }
          break;

        case WaitingForOperand_1:
          if(t.getType() == StartExpr) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_1;
          } else if(t.getType() == Operator && ( is(Plus, t) || is(Minus, t))) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForUnaryOperand;
          } else if(t.getType() == Operand) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperator_1;
          } else if (t.getType() == EndExpr) {
            if(s.isEmpty()) {
              state = Error;
            } else {
              EvalNode complement = s.pop();
              if(complement.getToken().getType() == StartExpr) {
                state = complement.previousState;
                // TERMINAL ( because there was nothing to evaluate )
                apply(Token.NULL_TOKEN);
              } else {
                state = Error;
              }
            }
          } else {
            state = Error;
          }
          break;

        case WaitingForOperand_2:
          if(t.getType() == StartExpr) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_1;
          } else if(t.getType() == Operator && ( is(Plus, t) || is(Minus, t))) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForUnaryOperand;
          } else if(t.getType() == Operand) {
            EvalNode op = s.pop();
            if (op.getToken().getOperator() == Mul || op.getToken().getOperator() == Div) {
              EvalNode op1 = s.pop();
              int value = eval(op.getToken().getOperator(), op1.getToken().getOperand(), t.getOperand());
              s.push(EvalNode.fromEvalNode(new Token(Operand, NA, value), op1.previousState));
              state = WaitingForOperator_1;
            } else {
              s.push(op);
              s.push(EvalNode.fromEvalNode(t, state));
              state = WaitingForOperator_2;
            }
          } else {
            state = Error;
          }
          break;

        case WaitingForOperand_3:
          if(t.getType() == StartExpr) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_1;
          } else if(t.getType() == Operator && ( is(Plus, t) || is(Minus, t))) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForUnaryOperand;
          } else if(t.getType() == Operand) {
            EvalNode op = s.pop();
            Validate.isTrue(op.getToken().getOperator() == Mul || op.getToken().getOperator() == Div);
            EvalNode op1 = s.pop();
            int value = eval(op.getToken().getOperator(), op1.getToken().getOperand(), t.getOperand());
            s.push(EvalNode.fromEvalNode(new Token(Operand, NA, value), op1.previousState));
            state = WaitingForOperator_2;
          } else {
            state = Error;
          }
          break;

        case WaitingForOperator_1:
          if(t.getType() == Operator) {
            s.push(EvalNode.fromEvalNode(t, state));
            state = WaitingForOperand_2;
          } else if(t.getType() == EndExpr) {
            EvalNode op1 = s.pop();
            EvalNode startExpr = s.pop();
            Validate.isTrue(startExpr.getToken().getType() == StartExpr);
            state = startExpr.previousState;
            apply(op1.getToken());
          } else {
            state = Error;
          }
          break;

        case WaitingForOperator_2:
          if(t.getType() == Operator) {
            Operator op = t.getOperator();
            if (op == Plus || op == Minus) {
              EvalNode op2 = s.pop();
              EvalNode operator = s.pop();
              EvalNode op1 = s.pop();
              int value = eval(
                  operator.getToken().getOperator(),
                  op1.getToken().getOperand(),
                  op2.getToken().getOperand());
              s.push(EvalNode.fromEvalNode(new Token(Operand, NA, value), op1.previousState));
              s.push(EvalNode.fromEvalNode(t, state));
              state = WaitingForOperand_2;
            } else {
              s.push(EvalNode.fromEvalNode(t, state));
              state = WaitingForOperand_3;
            }
          } else if (t.getType() == EndExpr) {
            EvalNode op2 = s.pop();
            EvalNode operator = s.pop();
            EvalNode op1 = s.pop();
            int value = eval(
                operator.getToken().getOperator(),
                op1.getToken().getOperand(),
                op2.getToken().getOperand());
            EvalNode startExpr = s.pop();
            state = startExpr.previousState;
            apply(new Token(Operand, NA, value));
          } else {
            state = Error;
          }
          break;

        default:
          state = Error;
          break;
      }
    }

    private int eval(Operator operator, int op1, int op2) {
      switch (operator) {
        case Plus:
          return op1 + op2;
        case Minus:
          return op1 - op2;
        case Mul:
          return op1 * op2;
        case Div:
          return op1 / op2;
        default:
          return 1;
      }
    }

    private boolean is(Operator op, Token t) {
      return t.getOperator() == op;
    }

    private int eval(Operator operator, int operand) {
      Validate.isTrue(operator == Plus || operator == Minus);
      switch (operator) {
        case Plus:
          return operand;
        case Minus:
          return -1 * operand;
        default:
          return 1;
      }
    }

    public boolean isError() {
      return state == Error;
    }

    public int last() {
      return s.peek().getToken().getOperand();
    }
  }

  @Value
  static class Token {
    TokenType type;
    Operator operator;
    int operand;

    static final Token INVALID_TOKEN = new Token(EndExpr, NA, 0);
    static final Token NULL_TOKEN = new Token(EndExpr, NA, 0);

    static Token fromTokenString(String s) {
      if (isNumber(s)) {
        return new Token(Operand, NA, numericalValue(s));
      } else if (s.compareTo("(") == 0) {
        return new Token(StartExpr, NA, 0);
      } else if (s.compareTo(")") == 0) {
        return new Token(EndExpr, NA, 0);
      } else if (s.compareTo("+") == 0) {
        return new Token(Operator, Plus, 0);
      } else if (s.compareTo("-") == 0) {
        return new Token(Operator, Minus, 0);
      } else if (s.compareTo("*") == 0) {
        return new Token(Operator, Mul, 0);
      } else if (s.compareTo("/") == 0) {
        return new Token(Operator, Div, 0);
      } else {
        return INVALID_TOKEN;
      }
    }

    private static int numericalValue(String s) {
      return Integer.parseInt(s);
    }

    private static boolean isNumber(String s) {
      try {
        Integer.parseInt(s);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }

    @Value
  static class EvalNode {
    Token token;
    EvalState previousState;

    static EvalNode fromEvalNode(Token t, EvalState prevState) {
      return new EvalNode(t, prevState);
    }
  }

  static enum TokenType {
    StartExpr,
    EndExpr,
    Operand,
    Operator
  }

  static enum Operator {
    Plus,
    Minus,
    Mul,
    Div,
    NA
  }

  static enum EvalState {
    Start,
    WaitingForOperand_1,
    WaitingForOperand_2,
    WaitingForOperand_3,
    WaitingForOperator_1,
    WaitingForOperator_2,
    WaitingForUnaryOperand,
    Error,
    Terminal
  }
}

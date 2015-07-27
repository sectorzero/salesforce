package org.sectorzero;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InfixEvaluationTest {

  InfixEvaluation evaluator;

  @Before
  public void setup() {
    evaluator = new InfixEvaluation();
  }

  @Test
  public void eval_1() throws Exception {
    e("2+3", 5);
  }

  @Test
  public void eval_2() throws Exception {
    e("(2+3)", 5);
  }

  @Test
  public void eval_3() throws Exception {
    e("(2+3-1)", 4);
  }

  @Test
  public void eval_4() throws Exception {
    e("(2+3*4)", 14);
  }

  @Test
  public void eval_5() throws Exception {
    e("(-2+3*4)", 10);
  }

  @Test
  public void eval_6() throws Exception {
    e("-9", -9);
  }

  @Test
  public void eval_7() throws Exception {
    e("4*1000", 4000);
  }

  @Test
  public void eval_8() throws Exception {
    e("4*(3+2)", 20);
  }

  @Test
  public void eval_9() throws Exception {
    e("16-24+6*2/-4", -11);
  }

  @Test
  public void eval_10() throws Exception {
    e("-1+-(-1)", 0);
  }

  void e(String expr, int expectedValue) throws Exception {
    assertEquals(expectedValue, evaluator.eval(expr));
  }
}

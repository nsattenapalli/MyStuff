
public class JavaEight extends AbstractClass{
	void test() {
		System.out.println("calling abstract class by override");
	}
	public static void main(String[] args) {
	Java8 j8 = new Java8() {
		@Override
		public void test() {
			System.out.println("calling interface");
		}
	};
	j8.test();
	
	Java8 lambda=()->{
		System.out.println("calling interface Using Lambda");
	};
	lambda.test();
	AbstractClass ac = new AbstractClass() {
		@Override
		void test() {
			System.out.println("calling abstract class");
		}
	};
	ac.test();
	JavaEight je = new JavaEight();
	je.test();
	}
}

public class JavaEight extends AbstractClass{
	//Modified
	void test() {
		System.out.println("calling abstract class by override");
	}
	public int trycatch() {
		try {
			return 0;
		}
		catch(Exception e) {
			e.printStackTrace();
		}finally {
			System.out.println("finally block");
		}
		return 1;
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
	System.out.println(je.trycatch());
	}
}
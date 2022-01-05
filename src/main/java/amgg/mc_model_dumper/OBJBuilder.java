package amgg.mc_model_dumper;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.EmptyStackException;
import java.util.Stack;

public class OBJBuilder extends AMGGStringBuilderBase<OBJBuilder> {
    protected Stack<Matrix4f> matrixStack = new Stack<>();

    public OBJBuilder() {
        super();
        matrixStack.add(new Matrix4f());
    }

    @Override
    protected OBJBuilder self() {
        return this;
    }

    OBJBuilder vert(float x, float y, float z) {
        Vector4f v1 = new Vector4f(x, y, z, 1f);
        Vector4f v2 = new Vector4f();
        Matrix4f.transform(matrixStack.peek(), v1, v2);
        addFormat("\nv %f %f %f %f", v2.x, v2.y, v2.z, v2.w);
        return this;
    }

    OBJBuilder vert_uv(double u, double v) {
        addFormat("\nvt %f %f", u, v);
        return this;
    }

    // ==== matrix stack stuff ====
    public OBJBuilder pushMatrix(Matrix4f mat) {
        Matrix4f newmat = new Matrix4f();
        Matrix4f.mul(matrixStack.peek(), mat, newmat);
        matrixStack.push(newmat);
        return this;
    }

    public OBJBuilder pushRotate(float angle, float axisx, float axisy, float axisz) {
        return pushMatrix(new Matrix4f().rotate(angle, new Vector3f(axisx, axisy, axisz)));
    }

    public OBJBuilder pushTranslate(float x, float y, float z) {
        return pushMatrix(new Matrix4f().translate(new Vector3f(x, y, z)));
    }

    public OBJBuilder popMatrix() throws EmptyStackException {
        matrixStack.pop();
        return this;
    }

    public OBJBuilder addModel(ModelBox box) {
        return this
            .vert(box.posX1, box.posY1, box.posZ1)
            .vert(box.posX2, box.posY1, box.posZ1)
            .vert(box.posX2, box.posY2, box.posZ1)
            .vert(box.posX1, box.posY2, box.posZ1)
            .vert(box.posX1, box.posY1, box.posZ2)
            .vert(box.posX2, box.posY1, box.posZ2)
            .vert(box.posX2, box.posY2, box.posZ2)
            .vert(box.posX1, box.posY2, box.posZ2)
            .add("\nf -8 -7 -6 -5")
            .add("\nf -4 -3 -2 -1")
            .add("\nf -8 -5 -1 -4")
            .add("\nf -7 -6 -2 -3")
            .add("\nf -6 -5 -1 -2")
            .add("\nf -8 -7 -3 -4");
    }

    public OBJBuilder addModel(ModelRenderer renderer) {
        return this
            .pushMatrix(new Matrix4f()
                    .translate(new Vector3f(renderer.offsetX, renderer.offsetY, renderer.offsetZ))
                    .translate(new Vector3f(renderer.rotationPointX, renderer.rotationPointY, renderer.rotationPointZ))
                    .rotate(renderer.rotateAngleZ * (180f / (float) Math.PI), new Vector3f(0f, 0f, 1f))
                    .rotate(renderer.rotateAngleY * (180f / (float) Math.PI), new Vector3f(0f, 1f, 0f))
                    .rotate(renderer.rotateAngleX * (180f / (float) Math.PI), new Vector3f(1f, 0f, 0f))
//                    .translate(new Vector3f(-renderer.offsetX, -renderer.offsetY, -renderer.offsetZ))
            )
            .forEach(renderer.cubeList, OBJBuilder::addModel)
            .forEach(renderer.childModels, OBJBuilder::addModel)
            .popMatrix();
    }
}

<?php

namespace App\Http\Controllers\Admin;

use App\Category;
use App\Http\Controllers\Controller;
use App\Http\Requests\CategoryRequest;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Session;

class CategoryController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function index()
    {
        $categories = Category::all();
        return view('admin.category.index',compact('categories'));
    }

    /**
     * Show the form for creating a new resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function create()
    {
        return view('admin.category.create');
    }

    /**
     * Store a newly created resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\Response
     */
    public function store(CategoryRequest $request)
    {
        $category = new Category();
        $category->name = $request->input('name');
        if ($request->input('slug')){
            $category->slug = make_slug(($request->input('slug')));
        }
        else{
            $category->slug = make_slug(($request->input('name')));
        }
        $category->description = $request->input('description');
        $category->meta_desc = $request->input('meta_desc');
        $category->meta_key = $request->input('meta_key');
        $category->save();
        Session::flash('success', 'دسته بندی مورد نظر با موفقیت ثبت گردید.');
        return   redirect(route('categories.index'));
    }

    /**
     * Display the specified resource.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function show($id)
    {
        //
    }

    /**
     * Show the form for editing the specified resource.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function edit($id)
    {
        $category = Category::findOrFail($id);
        return view('admin.category.edit',compact('category'));
    }

    /**
     * Update the specified resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function update(Request $request, $id)
    {
        $category = Category::findOrFail($id);
        $category->name = $request->input('name');
        if ($request->input('slug')){
            $category->slug = make_slug(($request->input('slug')));
        }
        else{
            $category->slug = make_slug(($request->input('name')));
        }
        $category->description = $request->input('description');
        $category->meta_desc = $request->input('meta_desc');
        $category->meta_key = $request->input('meta_key');
        $category->save();
        Session::flash('success', 'دسته بندی مورد نظر با موفقیت ویرایش گردید.');
        return   redirect(route('categories.index'));
    }

    /**
     * Remove the specified resource from storage.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function destroy($id)
    {
        $category = Category::findOrFail($id);
        $category->delete();
        Session::flash('delete', 'دسته بندی مورد نظر با موفقیت حذف گردید.');
        return redirect('/admin/categories');
    }
}

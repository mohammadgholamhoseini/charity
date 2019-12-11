@extends('admin.layout.master')
@section('content')

    <section class="content-header">
        @include('admin.layout.form-errors')
        <h1>
            ایجاد دسته بندی جدید
        </h1>
        <div class="col-md-10 offset-md-1">
            <form method="POST" action="{{route('categories.update',[$category->id])}}">
                @csrf
                <input type="hidden" name="_method" value="PATCH">
                <div class="form-group">
                    <label>عنوان</label>
                    <input type="text" class="form-control r" name="name" value="{{$category->name}}">
                </div>

                <div class="form-group">
                    <label>نام مستعار</label>
                    <input type="text" class="form-control" name="slug" value="{{$category->slug}}">
                </div>
                <div class="form-group">
                    <label>توضیحات</label>
                    <textarea class="form-control" name="description">{{$category->description}}</textarea>
                </div>
                <div class="form-group">
                    <label>متا-توضیحات</label>
                    <textarea class="form-control" name="meta_desc">{{$category->meta_desc}}</textarea>
                </div>

                <div class="form-group">
                    <label>متا-کلمات</label>
                    <textarea class="form-control" name="meta_key">{{$category->meta_key}}</textarea>
                </div>
                <div class="form-group">
                    <button type="submit" name="submit" class="btn btn-primary">ویرایش</button>
                </div>
            </form></div>
@endsection
